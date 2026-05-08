package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages batched placement of tree blocks across multiple server ticks.
 * Supports persistence: progress is saved to world data, so tree growth
 * continues even after server restart.
 *
 * <p>Processing is driven by {@link #onServerTick()}, which is called
 * from platform-specific tick hooks once per tick. This guarantees
 * exactly {@link #blocksPerTick()} blocks are placed per tick,
 * preventing server lag from large tree generation.
 */
public class TreePlacementTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("TreeGen");
    /** Blocks placed per tick. Configured in {@code config/sacred_trees.json} (default: 100000). */
    public static int blocksPerTick() { return SacredTreesConfig.blocksPerTick(); }
    /** Chunks synced to client per tick. Configured in {@code config/sacred_trees.json} (default: 200). */
    public static int chunksPerTick() { return SacredTreesConfig.chunksPerTick(); }
    private static int chunkRadius() { return SacredTreesConfig.chunkRadius(); }

    /** Queue of active tasks processed one batch per tick by {@link #onServerTick()}. */
    private static final Queue<TreePlacementTask> PENDING_TASKS = new ConcurrentLinkedQueue<>();
    /** Tick counter for progress reporting (~1 report/sec). */
    private static int progressReportTicker = 0;
    private static final int PROGRESS_INTERVAL_TICKS = 20;

    private final ServerLevel level;
    /** Compact primitive-array buffer for placement data (saves ~12-20 bytes/entry vs record). */
    private final PlacementBuffer placements;
    private final ChunkPos centerChunk;
    private int index;
    private MassiveTreeGenerator generator;
    private int syncChunkIdx = 0;
    /** Actual FORCED ticket radius used (may grow beyond chunkRadius() to cover all modified chunks). */
    private int effectiveChunkRadius;
    /** 0=placement, 1=lighting, 2=sync. */
    private int phase = 0;
    private final java.util.concurrent.atomic.AtomicInteger pendingLightCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final ArrayList<ChunkPos> modifiedChunks = new ArrayList<>();
    private final java.util.HashMap<ChunkPos, Integer> chunkMinY = new java.util.HashMap<>();
    private int blocksAdded = 0;
    private final long startTime;
    /** Optional pending tree reference for persistence. */
    private final TreeGenerationSavedData.PendingTree pendingTree;

    /**
     * Full constructor with persistence support.
     * @param pendingTree if non-null, progress is saved to world data after each batch
     */
    public TreePlacementTask(ServerLevel level, PlacementBuffer placements,
                            TreeGenerationSavedData.PendingTree pendingTree) {
        this.level = level;
        this.placements = placements;
        this.index = pendingTree != null ? pendingTree.progress : 0;
        this.pendingTree = pendingTree;
        this.startTime = System.currentTimeMillis();

        // Determine center chunk from first placement or sapling position
        if (!placements.isEmpty()) {
            BlockPos firstPos = BlockPos.of(placements.getPos(0));
            this.centerChunk = new ChunkPos(firstPos.getX() >> 4, firstPos.getZ() >> 4);
        } else if (pendingTree != null) {
            this.centerChunk = new ChunkPos(pendingTree.pos.getX() >> 4, pendingTree.pos.getZ() >> 4);
        } else {
            this.centerChunk = new ChunkPos(0, 0);
        }

        // Keep chunks force-loaded during tree generation to prevent
        // the sapling from disappearing when the player walks away.
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, centerChunk, chunkRadius());
        this.effectiveChunkRadius = chunkRadius();
    }

    public TreePlacementTask(ServerLevel level, PlacementBuffer placements) {
        this(level, placements, null);
    }

    /**
     * Called once per server tick from platform-specific tick hooks.
     * Processes one batch (blocksPerTick() blocks, or one chunk sync) from the active task.
     */
    public static void onServerTick() {
        TreePlacementTask task = PENDING_TASKS.poll();
        if (task == null) {
            progressReportTicker = 0;
            return;
        }

        // Phase 0: Generate placements incrementally (deferred from startPersistentGrowth)
        if (task.generator != null) {
            boolean genDone = task.generator.generateNextBatch(task.placements, blocksPerTick());
            if (!genDone) {
                // Show generation progress via action bar
                progressReportTicker++;
                if (progressReportTicker % PROGRESS_INTERVAL_TICKS == 0) {
                    int pct = (int) (task.generator.getGenerationProgress() * 100);
                    Component msg = Component.translatable(
                            "message.sacred_trees.generating_progress", pct);
                    for (ServerPlayer player : task.level.getPlayers(p -> true)) {
                        player.sendSystemMessage(msg, true);
                    }
                }
                // Still generating - re-add to queue, placement starts next tick after gen
                PENDING_TASKS.add(task);
                return;
            }
            // Generation complete!
            task.generator = null;
            task.placements.trimToSize();
            long blockCount = task.placements.size();
            LOGGER.info("Tree structure generated: {} blocks", blockCount);
            for (ServerPlayer player : task.level.getPlayers(p -> true)) {
                player.sendSystemMessage(Component.translatable(
                        "message.sacred_trees.start_placing",
                        task.pendingTree.pos.getX(), task.pendingTree.pos.getY(), task.pendingTree.pos.getZ(),
                        blockCount));
            }
            // Fall through to place the first batch this tick
        }

        boolean wasPlacement = task.phase == 0;

        if (task.phase == 2) {
            task.processSyncBatch();
        } else if (task.phase == 0) {
            task.processPlacementBatch();
        } else {
            task.processLightingBatch();
        }

        // Progress report via action bar (~1 report/sec)
        progressReportTicker++;
        if (progressReportTicker % PROGRESS_INTERVAL_TICKS == 0) {
            if (task.phase == 0) {
                // Show placement progress
                int totalBlocks = task.placements.size();
                if (totalBlocks <= 0) {
                    // Placement just finished this tick, buffer was cleared → 100%
                    totalBlocks = task.index;
                }
                int pct = (int) ((long) task.index * 100 / Math.max(1, totalBlocks));
                Component msg = Component.translatable(
                        "message.sacred_trees.progress",
                        task.index, totalBlocks, pct
                );
                for (ServerPlayer player : task.level.getPlayers(p -> true)) {
                    player.sendSystemMessage(msg, true);
                }
            } else if (task.phase == 1) {
                int remaining = task.pendingLightCount.get();
                Component msg = Component.translatable(
                        "message.sacred_trees.lighting_progress",
                        task.modifiedChunks.size() - remaining, task.modifiedChunks.size()
                );
                for (ServerPlayer player : task.level.getPlayers(p -> true)) {
                    player.sendSystemMessage(msg, true);
                }
            } else {
                // Show sync progress
                Component msg = Component.translatable(
                        "message.sacred_trees.sync_progress",
                        task.syncChunkIdx, task.modifiedChunks.size()
                );
                for (ServerPlayer player : task.level.getPlayers(p -> true)) {
                    player.sendSystemMessage(msg, true);
                }
            }
        }

        // If more work remains, re-add to queue for next tick
        if (!task.isDone()) {
            PENDING_TASKS.add(task);
        } else {
            task.finishGeneration();
        }
    }

    /**
     * First batch runs immediately via {@link #run()} (called from initial {@code execute()}).
     * Remaining batches are scheduled via the pending queue and processed by {@link #onServerTick()}.
     */
    @Override
    public void run() {
        processPlacementBatch();
        if (!isDone()) {
            PENDING_TASKS.add(this);
        }
    }

    /** Process one batch of blocksPerTick() blocks using direct section access (no light checks). */
    private void processPlacementBatch() {
        int end = Math.min(index + blocksPerTick(), placements.size());
        for (int i = index; i < end; i++) {
            BlockPos pos = BlockPos.of(placements.getPos(i));
            LevelChunk chunk = (LevelChunk) level.getChunk(pos);
            ChunkPos cp = chunk.getPos();
            Integer prevMinY = chunkMinY.get(cp);
            if (prevMinY == null) {
                chunkMinY.put(cp, pos.getY());
                modifiedChunks.add(cp);
                // Extend FORCED ticket radius to keep this chunk loaded.
                // Without this, chunks outside the initial radius get unloaded
                // when the player teleports away, causing blocking disk I/O.
                int dist = Math.max(Math.abs(cp.x() - centerChunk.x()), Math.abs(cp.z() - centerChunk.z()));
                if (dist > effectiveChunkRadius) {
                    level.getChunkSource().addTicketWithRadius(TicketType.FORCED, centerChunk, dist);
                    effectiveChunkRadius = dist;
                }
            } else if (pos.getY() < prevMinY) {
                chunkMinY.put(cp, pos.getY());
            }
            chunk.getSection(chunk.getSectionIndex(pos.getY())).setBlockState(
                    pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, placements.getState(i));
            chunk.markUnsaved();
            blocksAdded++;
        }
        index = end;

        if (pendingTree != null) {
            pendingTree.progress = index;
            TreeGenerationSavedData.get(level).setDirty();
        }

        if (index >= placements.size()) {
            // Placement done - submit per-chunk light tasks (avoids flooding the dispatcher
            // with 100k individual checkBlock calls per tick during placement).
            LOGGER.info("Tree placed {} blocks across {} chunks, lighting...",
                    blocksAdded, modifiedChunks.size());
            phase = 1;
            modifiedChunks.sort((a, b) -> {
                int cmp = Integer.compare(chunkMinY.getOrDefault(a, 0), chunkMinY.getOrDefault(b, 0));
                return cmp != 0 ? cmp : Integer.compare(a.x(), b.x());
            });
            if (placements.size() > 0) {
                placements.clear();
            }
            var lightEngine = level.getChunkSource().getLightEngine();
            pendingLightCount.set(modifiedChunks.size());
            for (ChunkPos cp : modifiedChunks) {
                LevelChunk chunk = (LevelChunk) level.getChunk(cp.getBlockAt(0, level.getMinY(), 0));
                if (chunk != null) {
                    ((net.minecraft.server.level.ThreadedLevelLightEngine) lightEngine)
                            .lightChunk(chunk, false)
                            .thenRun(() -> pendingLightCount.decrementAndGet());
                } else {
                    pendingLightCount.decrementAndGet();
                }
            }
        }
    }

    /** Wait for chunk light tasks to complete. Once all done, transitions to sync phase. */
    private void processLightingBatch() {
        if (pendingLightCount.get() <= 0) {
            phase = 2;
            syncChunkIdx = 0;
            LOGGER.info("Lighting complete, syncing {} chunks...", modifiedChunks.size());
        }
    }

    /** Sync up to {@link #chunksPerTick()} chunks to players who can see them. Called once per tick. */
    private void processSyncBatch() {
        for (int i = 0; i < chunksPerTick(); i++) {
            if (syncChunkIdx >= modifiedChunks.size()) {
                return; // isDone() will return true
            }

            ChunkPos cp = modifiedChunks.get(syncChunkIdx);
            LevelChunk chunk = (LevelChunk) level.getChunk(cp.getBlockAt(0, level.getMinY(), 0));
            if (chunk != null) {
                var packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
                for (ServerPlayer player : level.getPlayers(p -> p.getChunkTrackingView().contains(cp.x(), cp.z()))) {
                    player.connection.send(packet);
                }
            }
            syncChunkIdx++;
        }
        // Re-add to queue handled by onServerTick() if more chunks remain
    }

    /** Returns true when placement, lighting, and sync are all complete. */
    private boolean isDone() {
        if (phase < 2) return false;
        return syncChunkIdx >= modifiedChunks.size();
    }

    private void finishGeneration() {
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable("message.sacred_trees.done"));
        }
        LOGGER.info("Block sync complete");
        level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, centerChunk, effectiveChunkRadius);
        if (pendingTree != null) {
            TreeGenerationSavedData.get(level).removePending(pendingTree);
        }
        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.info(
                "Tree generation completed: {} blocks placed in {}ms",
                blocksAdded, elapsed
        );
    }

    // ========== Static helper methods ==========

    /**
     * Collect all block placements from a tree generator into a buffer.
     * Uses a fresh random seed for deterministic generation.
     */
    public static long collectPlacements(MassiveTreeGenerator generator,
                                          ServerLevel level,
                                          RandomSource random,
                                          BlockPos pos,
                                          PlacementBuffer output) {
        generator.startCollectMode();
        long seed = random.nextLong();
        boolean success = generator.generate(level, seed, pos);
        if (!success) return Long.MIN_VALUE;
        output.addAll(generator.stopCollectMode());
        output.trimToSize();
        return seed;
    }

    /**
     * Collect placements using a specific seed (for re-generation after reload).
     */
    public static boolean collectPlacementsWithSeed(MassiveTreeGenerator generator,
                                                     ServerLevel level,
                                                     long seed,
                                                     BlockPos pos,
                                                     PlacementBuffer output) {
        generator.startCollectMode();
        boolean success = generator.generate(level, seed, pos);
        if (!success) return false;
        output.addAll(generator.stopCollectMode());
        output.trimToSize();
        return true;
    }

    /**
     * Start batched tree growth with persistence support.
     * @return the PendingTree entry, or null if generation failed
     */
    public static TreeGenerationSavedData.PendingTree startPersistentGrowth(
            MassiveTreeGenerator generator,
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            String treeKind,
            boolean isFungus,
            AbstractSacredSapling.Type treeType) {

        // Check if a tree is already growing at this position (prevent duplicate bonemeal)
        TreeGenerationSavedData savedData = TreeGenerationSavedData.get(level);
        for (TreeGenerationSavedData.PendingTree existing : savedData.getPendingTrees()) {
            if (existing.pos.equals(pos)) {
                LOGGER.info("startPersistentGrowth SKIP at {}: already growing", pos);
                return existing; // Already growing here
            }
        }

        // Send immediate feedback before any heavy work
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable(
                    "message.sacred_trees.growing",
                    pos.getX(), pos.getY(), pos.getZ()));
        }

        // Save pending tree with seed (generation deferred to first tick)
        long seed = random.nextLong();
        TreeGenerationSavedData.PendingTree pending = new TreeGenerationSavedData.PendingTree(
                pos, seed, treeType, treeKind, isFungus, 0
        );
        savedData.addPending(pending);

        // Initialize incremental generation
        if (!generator.startIncrementalGen(level, seed, pos)) {
            // validTreeLocation failed
            savedData.removePending(pending);
            for (ServerPlayer player : level.getPlayers(p -> true)) {
                player.sendSystemMessage(Component.translatable(
                        "message.sacred_trees.failed",
                        pos.getX(), pos.getY(), pos.getZ()));
            }
            LOGGER.info("startPersistentGrowth FAIL at {}: validTreeLocation failed", pos);
            return null;
        }

        // Create task with empty buffer + generator reference
        // Generation will happen via generateNextBatch() in onServerTick()
        PlacementBuffer placements = new PlacementBuffer();
        TreePlacementTask task = new TreePlacementTask(level, placements, pending);
        task.generator = generator;
        PENDING_TASKS.add(task);
        return pending;
    }

    /**
     * Resume all pending tree generation tasks for a level.
     * Called on world load.
     */
    public static void resumePending(ServerLevel level) {
        TreeGenerationSavedData data = TreeGenerationSavedData.get(level);
        List<TreeGenerationSavedData.PendingTree> pending = new ArrayList<>(data.getPendingTrees());

        for (TreeGenerationSavedData.PendingTree tree : pending) {
            BlockPos saplingPos = tree.pos;
            BlockState saplingState = level.getBlockState(saplingPos);

            // Check if sapling still exists
            if (!(saplingState.getBlock() instanceof AbstractSacredSapling)) {
                // Sapling was removed - cancel the task
                data.removePending(tree);
                continue;
            }

            // Re-create the generator with same parameters
            MassiveTreeGenerator gen = createGeneratorForType(tree);
            if (gen == null) {
                data.removePending(tree);
                continue;
            }

            // Re-collect placements (deterministic - same seed = same tree)
            PlacementBuffer placements = new PlacementBuffer();
            boolean success = collectPlacementsWithSeed(gen, level, tree.seed, tree.pos, placements);
            if (!success || placements.isEmpty()) {
                data.removePending(tree);
                continue;
            }

            // Validate progress
            if (tree.progress >= placements.size()) {
                data.removePending(tree);
                continue;
            }

            // Resume from saved progress
            LOGGER.info("Resuming tree generation at {}: {}/{} blocks placed",
                    tree.pos, tree.progress, placements.size());

            TreePlacementTask task = new TreePlacementTask(level, placements, tree);
            level.getServer().execute(task);
        }
    }

    /**
     * Re-create a MassiveTreeGenerator from stored PendingTree parameters.
     */
    private static MassiveTreeGenerator createGeneratorForType(TreeGenerationSavedData.PendingTree tree) {
        String kind = tree.treeKind;
        boolean isFungus = tree.isFungus;

        // Build the tree blocks based on kind
        net.minecraft.world.level.block.Block log, wood, leaves;
        net.minecraft.world.level.block.Block lights = null, vines = null, vines2 = null;

        switch (kind) {
            case "oak": log = Blocks.OAK_LOG; wood = Blocks.OAK_WOOD; leaves = Blocks.OAK_LEAVES; break;
            case "birch": log = Blocks.BIRCH_LOG; wood = Blocks.BIRCH_WOOD; leaves = Blocks.BIRCH_LEAVES; break;
            case "spruce": log = Blocks.SPRUCE_LOG; wood = Blocks.SPRUCE_WOOD; leaves = Blocks.SPRUCE_LEAVES; break;
            case "jungle": log = Blocks.JUNGLE_LOG; wood = Blocks.JUNGLE_WOOD; leaves = Blocks.JUNGLE_LEAVES; break;
            case "acacia": log = Blocks.ACACIA_LOG; wood = Blocks.ACACIA_WOOD; leaves = Blocks.ACACIA_LEAVES; break;
            case "dark_oak": log = Blocks.DARK_OAK_LOG; wood = Blocks.DARK_OAK_WOOD; leaves = Blocks.DARK_OAK_LEAVES; break;
            case "cherry": log = Blocks.CHERRY_LOG; wood = Blocks.CHERRY_WOOD; leaves = Blocks.CHERRY_LEAVES; break;
            case "mangrove": log = Blocks.MANGROVE_LOG; wood = Blocks.MANGROVE_WOOD; leaves = Blocks.MANGROVE_LEAVES; break;
            case "pale_oak": log = Blocks.PALE_OAK_LOG; wood = Blocks.PALE_OAK_WOOD; leaves = Blocks.PALE_OAK_LEAVES; break;
            case "crimson":
                log = Blocks.CRIMSON_STEM; wood = Blocks.CRIMSON_HYPHAE; leaves = Blocks.NETHER_WART_BLOCK;
                lights = Blocks.SHROOMLIGHT; vines = Blocks.WEEPING_VINES_PLANT; vines2 = Blocks.WEEPING_VINES;
                break;
            case "warped":
                log = Blocks.WARPED_STEM; wood = Blocks.WARPED_HYPHAE; leaves = Blocks.WARPED_WART_BLOCK;
                lights = Blocks.SHROOMLIGHT;
                break;
            default: return null;
        }

        if (isFungus) {
            return new FungusTreeGenerator(
                    log.defaultBlockState(), wood.defaultBlockState(), leaves.defaultBlockState(),
                    lights != null ? lights.defaultBlockState() : null,
                    vines != null ? vines.defaultBlockState() : null,
                    vines2 != null ? vines2.defaultBlockState() : null
            );
        } else {
            if ("mangrove".equals(tree.treeKind)) {
                return new MangroveTreeGenerator(
                        log.defaultBlockState(), wood.defaultBlockState(), leaves.defaultBlockState()
                );
            }
            return new MassiveTreeGenerator(
                    log.defaultBlockState(), wood.defaultBlockState(), leaves.defaultBlockState()
            );
        }
    }
}
