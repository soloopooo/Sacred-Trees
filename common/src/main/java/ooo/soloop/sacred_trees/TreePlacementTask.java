package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages batched placement of tree blocks across multiple server ticks.
 * Supports persistence: progress is saved to world data, so tree growth
 * continues even after server restart.
 */
public class TreePlacementTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("TreeGen");
    /** Blocks placed per tick. Lower = smoother but slower. */
    public static final int BLOCKS_PER_TICK = 500;
    /** Blocks synced to client per tick. */
    private static final int SYNC_BLOCKS_PER_TICK = 50;
    private static final int CHUNK_RADIUS = 16;

    /** Represents a single block to place. Position stored as long (BlockPos.asLong). */
    public record PlacementEntry(long pos, BlockState state) {}

    private final ServerLevel level;
    private final List<PlacementEntry> placements;
    private final ChunkPos centerChunk;
    private int index;
    private int syncChunkIdx = 0;
    private long lastSyncTime = 0;
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
    public TreePlacementTask(ServerLevel level, List<PlacementEntry> placements,
                            TreeGenerationSavedData.PendingTree pendingTree) {
        this.level = level;
        this.placements = placements;
        this.index = pendingTree != null ? pendingTree.progress : 0;
        this.pendingTree = pendingTree;
        this.startTime = System.currentTimeMillis();

        // Determine center chunk from first placement
        if (!placements.isEmpty()) {
            BlockPos firstPos = BlockPos.of(placements.get(0).pos);
            this.centerChunk = new ChunkPos(firstPos.getX() >> 4, firstPos.getZ() >> 4);
        } else {
            this.centerChunk = new ChunkPos(0, 0);
        }

        // Keep chunks force-loaded during tree generation to prevent
        // the sapling from disappearing when the player walks away.
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, centerChunk, CHUNK_RADIUS);
    }

    public TreePlacementTask(ServerLevel level, List<PlacementEntry> placements) {
        this(level, placements, null);
    }

    @Override
    public void run() {
        if (placementDone) {
            // === Sync phase: one full chunk at a time, rate-limited ===
            if (syncChunkIdx >= modifiedChunks.size()) {
                finishGeneration();
                return;
            }
            // At most 5 chunks per second (200ms each)
            long now = System.currentTimeMillis();
            if (now - lastSyncTime < 1) {
                level.getServer().execute(this);
                return;
            }
            lastSyncTime = now;

            ChunkPos cp = modifiedChunks.get(syncChunkIdx);
            LevelChunk chunk = (LevelChunk) level.getChunk(cp.getBlockAt(0, level.getMinY(), 0));
            if (chunk != null) {
                for (ServerPlayer player : level.getPlayers(p -> true)) {
                    player.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null));
                }
            }
            syncChunkIdx++;
            level.getServer().execute(this);
            return;
        }

        // === Placement phase: place blocks in batches ===
        int end = Math.min(index + BLOCKS_PER_TICK, placements.size());
        for (int i = index; i < end; i++) {
            PlacementEntry entry = placements.get(i);
            BlockPos pos = BlockPos.of(entry.pos);
            ChunkPos cp = level.getChunk(pos).getPos();
            if (uniqueChunks.add(cp)) {
                modifiedChunks.add(cp);
            }
            level.getChunk(pos).setBlockState(pos, entry.state, 0);
            blocksAdded++;
        }
        index = end;

        if (pendingTree != null) {
            pendingTree.progress = index;
            TreeGenerationSavedData.get(level).setDirty();
        }

        if (index < placements.size()) {
            level.getServer().execute(this);
        } else {
            // Placement done - move to sync phase
            LOGGER.info("Tree placed {} blocks across {} chunks, syncing to client...",
                    blocksAdded, modifiedChunks.size());
            placementDone = true;
            syncChunkIdx = 0;
            level.getServer().execute(this);
        }
    }

    private void finishGeneration() {
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable("message.sacred_trees.done"));
        }
        LOGGER.info("Block sync complete");
        level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, centerChunk, CHUNK_RADIUS);
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
     * Collect all block placements from a tree generator into a list.
     * Uses a fresh random seed for deterministic generation.
     */
    public static long collectPlacements(MassiveTreeGenerator generator,
                                          ServerLevel level,
                                          RandomSource random,
                                          BlockPos pos,
                                          List<PlacementEntry> output) {
        generator.startCollectMode();
        long seed = random.nextLong();
        boolean success = generator.generate(level, seed, pos);
        if (!success) return Long.MIN_VALUE;
        output.addAll(generator.stopCollectMode());
        return seed;
    }

    /**
     * Collect placements using a specific seed (for re-generation after reload).
     */
    public static boolean collectPlacementsWithSeed(MassiveTreeGenerator generator,
                                                     ServerLevel level,
                                                     long seed,
                                                     BlockPos pos,
                                                     List<PlacementEntry> output) {
        generator.startCollectMode();
        boolean success = generator.generate(level, seed, pos);
        if (!success) return false;
        output.addAll(generator.stopCollectMode());
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

        List<PlacementEntry> placements = new ArrayList<>();
        long seed = collectPlacements(generator, level, random, pos, placements);
        if (seed == Long.MIN_VALUE || placements.isEmpty()) {
            for (ServerPlayer player : level.getPlayers(p -> true)) {
                player.sendSystemMessage(Component.translatable(
                        "message.sacred_trees.failed",
                        pos.getX(), pos.getY(), pos.getZ()));
            }
            LOGGER.info("startPersistentGrowth FAIL at {}: collectPlacements returned {} placements (seed={})",
                    pos, placements.size(), seed);
            return null;
        }

        // Save pending tree to world data
        long blockCount = placements.size();
        for (ServerPlayer player : level.getPlayers(p -> true)) {
            player.sendSystemMessage(Component.translatable(
                    "message.sacred_trees.growing",
                    pos.getX(), pos.getY(), pos.getZ(), blockCount));
        }

        TreeGenerationSavedData.PendingTree pending = new TreeGenerationSavedData.PendingTree(
                pos, seed, treeType, treeKind, isFungus, 0
        );
        TreeGenerationSavedData.get(level).addPending(pending);

        // Start batch placement
        TreePlacementTask task = new TreePlacementTask(level, placements, pending);
        level.getServer().execute(task);
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
            List<PlacementEntry> placements = new ArrayList<>();
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
            return new MassiveTreeGenerator(
                    log.defaultBlockState(), wood.defaultBlockState(), leaves.defaultBlockState(),
                    lights != null ? lights.defaultBlockState() : null,
                    vines != null ? vines.defaultBlockState() : null,
                    vines2 != null ? vines2.defaultBlockState() : null
            );
        } else {
            return new MassiveTreeGenerator(
                    log.defaultBlockState(), wood.defaultBlockState(), leaves.defaultBlockState()
            );
        }
    }
}
