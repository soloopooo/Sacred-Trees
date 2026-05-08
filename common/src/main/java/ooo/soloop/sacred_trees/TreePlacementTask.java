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
    private boolean placementDone = false;
    private final ArrayList<ChunkPos> modifiedChunks = new ArrayList<>();
    private final java.util.HashSet<ChunkPos> uniqueChunks = new java.util.HashSet<>();
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

        // Phase 0: Generate placements if not yet done (deferred from startPersistentGrowth)
        if (task.generator != null) {
            task.generateTree();
            task.generator = null; // Done generating
            // Fall through to place the first batch in the same tick
        }

        boolean wasPlacement = !task.placementDone;

        if (task.placementDone) {
            task.processSyncBatch();
        } else {
            task.processPlacementBatch();
        }

        // Progress report via action bar (~1 report/sec)
        progressReportTicker++;
        if (progressReportTicker % PROGRESS_INTERVAL_TICKS == 0) {
            if (wasPlacement) {
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
     * Generate tree placements (called from onServerTick on first batch).
     * Runs collectPlacements with the stored generator, then populates the buffer.
     */
    private void generateTree() {
        LOGGER.info("Generating tree structure at {}...", pendingTree.pos);
        boolean success = collectPlacementsWithSeed(
                generator, level, pendingTree.seed, pendingTree.pos, placements);
        if (!success || placements.isEmpty()) {
            LOGGER.info("Tree generation FAIL at {}: no placements", pendingTree.pos);
            for (ServerPlayer player : level.getPlayers(p -> true)) {
                player.sendSystemMessage(Component.translatable(
                        "message.sacred_trees.failed",
                        pendingTree.pos.getX(), pendingTree.pos.getY(), pendingTree.pos.getZ()));
            }
            // Mark done so cleanup happens
            placementDone = true;
            modifiedChunks.clear();
            return;
        }
        placements.trimToSize();
        long blockCount = placements.size();
        LOGGER.info("Tree structure generated: {} blocks", blockCount);
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable(
                    "message.sacred_trees.start_placing",
                    pendingTree.pos.getX(), pendingTree.pos.getY(), pendingTree.pos.getZ(),
                    blockCount));
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

    /** Process one batch of blocksPerTick() blocks. */
    private void processPlacementBatch() {
        int end = Math.min(index + blocksPerTick(), placements.size());
        for (int i = index; i < end; i++) {
            BlockPos pos = BlockPos.of(placements.getPos(i));
            ChunkPos cp = level.getChunk(pos).getPos();
            if (uniqueChunks.add(cp)) {
                modifiedChunks.add(cp);
            }
            level.getChunk(pos).setBlockState(pos, placements.getState(i), 0);
            blocksAdded++;
        }
        index = end;

        if (pendingTree != null) {
            pendingTree.progress = index;
            TreeGenerationSavedData.get(level).setDirty();
        }

        if (index >= placements.size()) {
            // Placement done - free the placement buffer before sync phase
            LOGGER.info("Tree placed {} blocks across {} chunks, syncing to client...",
                    blocksAdded, modifiedChunks.size());
            placementDone = true;
            syncChunkIdx = 0;
            // Allow GC to reclaim the placement buffer (~1-2 GB for huge trees)
            if (placements.size() > 0) {
                placements.clear();
            }
        }
    }

    /** Sync up to {@link #chunksPerTick()} chunks to all online players. Called once per tick. */
    private void processSyncBatch() {
        for (int i = 0; i < chunksPerTick(); i++) {
            if (syncChunkIdx >= modifiedChunks.size()) {
                return; // isDone() will return true
            }

            ChunkPos cp = modifiedChunks.get(syncChunkIdx);
            LevelChunk chunk = (LevelChunk) level.getChunk(cp.getBlockAt(0, level.getMinY(), 0));
            if (chunk != null) {
                for (ServerPlayer player : level.getPlayers(p -> true)) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
                }
            }
            syncChunkIdx++;
        }
        // Re-add to queue handled by onServerTick() if more chunks remain
    }

    /** Returns true when both placement and sync are complete. */
    private boolean isDone() {
        return placementDone && syncChunkIdx >= modifiedChunks.size();
    }

    private void finishGeneration() {
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable("message.sacred_trees.done"));
        }
        LOGGER.info("Block sync complete");
        level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, centerChunk, chunkRadius());
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
        TreeGenerationSavedData.get(level).addPending(pending);

        // Create task with empty buffer + generator reference
        // Generation will happen in onServerTick() on the next tick
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
