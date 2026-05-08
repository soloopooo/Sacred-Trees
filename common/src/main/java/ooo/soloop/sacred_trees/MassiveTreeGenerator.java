package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MassiveTreeGenerator {
    private static final Logger DEBUG_LOGGER = LoggerFactory.getLogger("TreeGenDebug");
    // Parametric blockstates to use
    public BlockState leaves;
    /** Persistent (non-decaying) variant of leaves - prevents leaf decay */
    public BlockState persistentLeaves;
    public BlockState log;
    public BlockState wood;
    public BlockState lights = null;
    public BlockState vines = null;
    public BlockState vines2 = null;
    public boolean genLightsAndVines = false;

    // Collect mode for batched placement
    private boolean collectMode = false;
    private PlacementBuffer collectedPlacements = new PlacementBuffer();

    /** Switch to collect mode: instead of placing blocks, record them for batched placement. */
    public void startCollectMode() {
        this.collectMode = true;
        this.collectedPlacements.clear();
    }

    /** Stop collect mode and return the buffer of collected placements. */
    public PlacementBuffer stopCollectMode() {
        this.collectMode = false;
        PlacementBuffer result = this.collectedPlacements;
        // Allocate a fresh buffer — the caller now owns the old one
        this.collectedPlacements = new PlacementBuffer();
        return result;
    }

    // ========== Incremental generation ==========

    private int genPhase;         // 1=leafList, 2=leaves, 3=bases, 4=trunk, 5=done
    private int genBlockBudget;   // blocks to generate before yielding
    /** State for incremental leaf node list generation. */
    private int genListCurrentY, genListNodeCount, genListTopY, genListHeightOffset;
    private int genListDensityIdx;
    private int genLeafIdx;

    /** Start incremental generation. Returns false if tree can't grow here. */
    public boolean startIncrementalGen(Level world, long seed, BlockPos pos) {
        this.world = world;
        rand = RandomSource.create(seed);
        basePos[0] = pos.getX();
        basePos[1] = pos.getY();
        basePos[2] = pos.getZ();
        if (heightLimit == 0) heightLimit = heightLimitLimit;
        if (minHeight == -1) minHeight = 80;

        if (!this.validTreeLocation()) return false;
        this.setup();

        collectMode = true;
        collectedPlacements.clear();
        genPhase = 1;
        genBlockBudget = 0;

        // Init leaf list state
        int densityCount = density;
        genListNodes = new int[densityCount * heightLimit][4];
        genListCurrentY = basePos[1] + heightLimit - leafDistanceLimit;
        genListNodeCount = 1;
        genListTopY = basePos[1] + height;
        genListHeightOffset = genListCurrentY - basePos[1];
        genListNodes[0][0] = basePos[0];
        genListNodes[0][1] = genListCurrentY;
        genListNodes[0][2] = basePos[2];
        genListNodes[0][3] = genListTopY;
        --genListCurrentY;
        genListDensityIdx = 0;
        genLeafIdx = 0;

        return true;
    }

    /** Temporary storage for incremental leaf node list. */
    private int[][] genListNodes;

    /**
     * Generate the next batch of placements.
     * @param output buffer to append generated placements to
     * @param maxBlocks approximate max blocks to generate this batch
     * @return true when generation is fully complete
     */
    public boolean generateNextBatch(PlacementBuffer output, int maxBlocks) {
        genBlockBudget = maxBlocks;

        while (genBlockBudget > 0 && genPhase < 5) {
            switch (genPhase) {
                case 1 -> tickLeafNodeList();
                case 2 -> tickLeaves();
                case 3 -> { generateLeafNodeBases(); genPhase = 4; }
                case 4 -> { generateTrunk(new BlockPos(basePos[0], basePos[1], basePos[2])); genPhase = 5; }
            }
        }

        if (collectedPlacements.size() > 0) {
            output.addAll(collectedPlacements);
            collectedPlacements.clear();
        }
        return genPhase >= 5;
    }

    /**
     * Estimated generation progress 0.0 ~ 1.0.
     * Phase 1 (leaf list): 0% ~ 85%, Phase 2 (leaves): 85% ~ 98%, rest: ~100%.
     */
    public float getGenerationProgress() {
        switch (genPhase) {
            case 1: {
                int totalHeightSteps = heightLimit - leafDistanceLimit;
                if (totalHeightSteps <= 0) return 0.85f;
                return 0.85f * (1.0f - (float) (genListHeightOffset + 1) / totalHeightSteps);
            }
            case 2: {
                if (leafNodesLength <= 0) return 0.98f;
                return 0.85f + 0.13f * (float) genLeafIdx / leafNodesLength;
            }
            case 3:
            case 4:
                return 0.99f;
            default:
                return 1.0f;
        }
    }

    /** Run one tick of leaf node list generation. */
    private void tickLeafNodeList() {
        int[] basePos = this.basePos;
        int densityCount = density;
        int[][] nodes = genListNodes;
        int currentY = genListCurrentY;
        int nodeCount = genListNodeCount;
        int topY = genListTopY;
        int heightOffset = genListHeightOffset;
        int densityIdx = genListDensityIdx;

        while (heightOffset >= 0 && genBlockBudget > 0) {
            float layerRadius = this.layerSize(heightOffset);
            if (layerRadius > 0.0F) {
                float halfOffset = 0.5f;
                for (; densityIdx < densityCount && genBlockBudget > 0; ++densityIdx) {
                    float branchRadius = scaleWidth * layerRadius * (rand.nextFloat() + 0.328f);
                    float branchAngle = rand.nextFloat() * 2.0f * PI;
                    int posX = Mth.floor(branchRadius * Math.sin(branchAngle) + basePos[0] + halfOffset);
                    int posZ = Mth.floor(branchRadius * Math.cos(branchAngle) + basePos[2] + halfOffset);
                    int[] leafPos = new int[]{posX, currentY, posZ};
                    int[] leafTop = new int[]{posX, currentY + leafDistanceLimit, posZ};
                    if (this.checkBlockLine(leafPos, leafTop) == -1) {
                        int t;
                        double distance = Math.sqrt((t = basePos[0] - leafPos[0]) * t + (t = basePos[2] - leafPos[2]) * t);
                        int yOffset = (int) (distance * branchSlope);
                        int[] branchBase = new int[]{basePos[0], Math.min(leafPos[1] - yOffset, topY), basePos[2]};
                        if (this.checkBlockLine(branchBase, leafPos) == -1) {
                            nodes[nodeCount][0] = posX;
                            nodes[nodeCount][1] = currentY;
                            nodes[nodeCount][2] = posZ;
                            nodes[nodeCount][3] = branchBase[1];
                            ++nodeCount;
                        }
                    }
                    genBlockBudget--;
                }
            }
            // Advance height only if density loop completed (ran all iters, or layerRadius <= 0)
            // If budget exhausted mid-density, break without advancing height
            if (densityIdx >= densityCount || genBlockBudget > 0) {
                densityIdx = 0;
                --currentY;
                --heightOffset;
            } else {
                break; // Budget exhausted mid-density, yield now
            }
        }
        genListCurrentY = currentY; genListNodeCount = nodeCount; genListTopY = topY; genListHeightOffset = heightOffset;
        genListDensityIdx = densityIdx;
        if (heightOffset < 0) {
            leafNodes = nodes; leafNodesLength = nodeCount;
            genLeafIdx = 0;
            genPhase = 2;
        }
    }

    /** Run one tick of leaf generation. */
    private void tickLeaves() {
        int[][] leafNodes = this.leafNodes;
        for (; genLeafIdx < leafNodesLength && genBlockBudget > 0; genLeafIdx++) {
            int[] node = leafNodes[genLeafIdx];
            int x = node[0], nodeY = node[1], z = node[2];
            int blocksBefore = collectedPlacements.size();
            setBlockAndNotifyAdequately(world, x, nodeY, z, wood);
            int leafY = nodeY;
            for (int y = 0; y < leafDistanceLimit; y++) {
                int size = (y != 0) && y != leafDistanceLimit - 1 ? 3 : 2;
                genLeafLayer(x, leafY++, z, size);
            }
            // Consume budget by actual blocks added to buffer
            genBlockBudget -= (collectedPlacements.size() - blocksBefore);
        }
        if (genLeafIdx >= leafNodesLength) genPhase = 3;
    }

    private static final byte[] otherCoordPairs = new byte[]{(byte) 2, (byte) 0, (byte) 0, (byte) 1, (byte) 2, (byte) 1};
    private static final float PI = (float) Math.PI;

    protected RandomSource rand = RandomSource.create();

    /** Running variables */
    protected Level world;
    private int[] basePos = new int[]{0, 0, 0};
    protected int heightLimit = 0;
    private int minHeight = -1;
    private int height;
    private int leafBases;
    private int density;

    /** Setup variables */
    private float heightAttenuation = 0.45f;
    private float branchSlope = 0.45f;
    private float scaleWidth = 4.0f;
    private float branchDensity = 3.0f;
    private int trunkSize = 11;
    private boolean slopeTrunk = false;
    private boolean safeGrowth = false;

    private int heightLimitLimit = 250;
    private int leafDistanceLimit = 4;
    private int leafNodesLength;
    private int[][] leafNodes;

    private static BlockState makePersistent(BlockState leaves) {
        if (leaves.hasProperty(BlockStateProperties.PERSISTENT)) {
            return leaves.setValue(BlockStateProperties.PERSISTENT, true);
        }
        return leaves;
    }

    public MassiveTreeGenerator(BlockState log, BlockState wood, BlockState leaves) {
        this.leaves = leaves;
        this.persistentLeaves = makePersistent(leaves);
        this.log = log;
        this.wood = wood;
    }

    public MassiveTreeGenerator(BlockState log, BlockState wood, BlockState leaves,
                                BlockState lights, BlockState vines, BlockState vines2) {
        this.leaves = leaves;
        this.persistentLeaves = makePersistent(leaves);
        this.log = log;
        this.wood = wood;
        this.lights = lights;
        this.vines = vines;
        this.vines2 = vines2;
        this.genLightsAndVines = true;
    }

    private void setup() {
        leafBases = Mth.ceil(heightLimit * heightAttenuation);
        density = Math.max(1, (int) (1.382D + Math.pow(branchDensity * heightLimit / 13.0D, 2.0D)));
    }

    private float layerSize(int layerHeight) {
        if (layerHeight < leafBases)
            return -1.618F;
        else {
            float halfHeight = heightLimit * 0.5F;
            float heightDiff = heightLimit * 0.5F - layerHeight;
            float layerWidth;
            if (heightDiff == 0.0F) {
                layerWidth = halfHeight;
            } else if (Math.abs(heightDiff) >= halfHeight) {
                return 0.0F;
            } else {
                layerWidth = (float) Math.sqrt(halfHeight * halfHeight - heightDiff * heightDiff);
            }
            layerWidth *= 0.5F;
            return layerWidth;
        }
    }

    private void generateLeafNodeList() {
        int densityCount = density;
        int[] basePos = this.basePos;
        int[][] nodes = new int[densityCount * heightLimit][4];
        int currentY = basePos[1] + heightLimit - leafDistanceLimit;
        int nodeCount = 1;
        int topY = basePos[1] + height;
        int heightOffset = currentY - basePos[1];
        nodes[0][0] = basePos[0];
        nodes[0][1] = currentY;
        nodes[0][2] = basePos[2];
        nodes[0][3] = topY;
        --currentY;

        while (heightOffset >= 0) {
            int densityIdx = 0;
            float layerRadius = this.layerSize(heightOffset);
            if (layerRadius > 0.0F) {
                float halfOffset = 0.5f;
                for (; densityIdx < densityCount; ++densityIdx) {
                    float branchRadius = scaleWidth * layerRadius * (rand.nextFloat() + 0.328f);
                    float branchAngle = rand.nextFloat() * 2.0f * PI;
                    int posX = Mth.floor(branchRadius * Math.sin(branchAngle) + basePos[0] + halfOffset);
                    int posZ = Mth.floor(branchRadius * Math.cos(branchAngle) + basePos[2] + halfOffset);
                    int[] leafPos = new int[]{posX, currentY, posZ};
                    int[] leafTop = new int[]{posX, currentY + leafDistanceLimit, posZ};

                    if (this.checkBlockLine(leafPos, leafTop) == -1) {
                        int t;
                        double distance = Math.sqrt(
                                (t = basePos[0] - leafPos[0]) * t +
                                (t = basePos[2] - leafPos[2]) * t);
                        int yOffset = (int) (distance * branchSlope);
                        int[] branchBase = new int[]{basePos[0], Math.min(leafPos[1] - yOffset, topY), basePos[2]};

                        if (this.checkBlockLine(branchBase, leafPos) == -1) {
                            nodes[nodeCount][0] = posX;
                            nodes[nodeCount][1] = currentY;
                            nodes[nodeCount][2] = posZ;
                            nodes[nodeCount][3] = branchBase[1];
                            ++nodeCount;
                        }
                    }
                }
            }
            --currentY;
            --heightOffset;
        }
        leafNodes = nodes;
        leafNodesLength = nodeCount;
    }

    private void genVines(Level world, int x, int y, int z, int length) {
        for (int i = 1; i < length; i++) {
            BlockPos pos = new BlockPos(x, y - i, z);
            if (!world.getBlockState(pos).isAir()) return;
            this.setBlockAndNotifyAdequately(world, x, y - i, z, vines);
        }
        if (vines2 != null) {
            BlockPos endPos = new BlockPos(x, y - length, z);
            if (world.getBlockState(endPos).isAir())
                this.setBlockAndNotifyAdequately(world, x, y - length, z, vines2);
        }
    }

    private void genLeafLayer(int x, int y, int z, final int size) {
        int sign;
        final int originX = x;
        final int originZ = z;
        final float maxDistSq = size * size;

        for (int dx = -size; dx <= size; ++dx) {
            x = originX + dx;
            final int dxSq = dx * dx + (((sign = dx >> 31) ^ dx) - sign);
            for (int dz = 0; dz <= size; ) {
                final float distSq = dxSq + dz * dz + dz + 0.5f;
                if (distSq > maxDistSq) {
                    break;
                } else {
                    sign = -1;
                    do {
                        z = originZ + dz * sign;
                        BlockPos placementPos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(placementPos);
                        Block block = state.getBlock();
                        BlockState blockToSet = persistentLeaves;

                        if (this.genLightsAndVines) {
                            float randFloat = rand.nextFloat();
                            if (randFloat < 0.03f && world.getBlockState(placementPos.below()).isAir()) {
                                if (this.lights != null && randFloat < 0.02f) blockToSet = lights;
                                else if (this.vines != null)
                                    this.genVines(world, x, y, z, rand.nextInt(this.height / 3));
                            }
                        }

                        if (safeGrowth ? canBeReplacedByLeaves(state, world, placementPos) :
                                block != Blocks.BEDROCK) {
                            this.setBlockAndNotifyAdequately(world, x, y, z, blockToSet);
                        }
                        if (sign == 1) break;
                        sign = 1;
                    } while (true);
                    ++dz;
                }
            }
        }
    }

    private static boolean canBeReplacedByLeaves(BlockState state, Level level, BlockPos pos) {
        return state.isAir() || state.canBeReplaced() ||
               state.is(Blocks.AIR) || state.getFluidState().is(Fluids.WATER);
    }

    protected static boolean canBeReplacedByLogs(BlockState state, Level level, BlockPos pos) {
        return state.isAir() || state.canBeReplaced() ||
               state.is(Blocks.AIR) || state.getFluidState().is(Fluids.WATER) ||
               state.getBlock() instanceof SaplingBlock;
    }

    private static boolean canSustainPlant(BlockState state, Level level, BlockPos pos) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) ||
               state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT) ||
               state.is(Blocks.FARMLAND) || state.is(Blocks.MYCELIUM) ||
               state.is(Blocks.SOUL_SOIL) || state.is(Blocks.CRIMSON_NYLIUM) ||
               state.is(Blocks.WARPED_NYLIUM);
    }

    private void generateLeaves() {
        int[][] leafNodes = this.leafNodes;
        for (int i = 0, len = leafNodesLength; i < len; ++i) {
            int[] node = leafNodes[i];
            int x = node[0], leafY = node[1], z = node[2];
            this.setBlockAndNotifyAdequately(world, x, leafY, z, wood);
            for (int layer = 0; layer < leafDistanceLimit; ++layer) {
                int size = (layer != 0) && layer != leafDistanceLimit - 1 ? 3 : 2;
                genLeafLayer(x, leafY++, z, size);
            }
        }
    }

    private int[] placeScratch = new int[3];

    private void placeBlockLine(int[] start, int[] end, BlockState logState, BlockState woodState) {
        int[] deltas = placeScratch;
        byte mainAxis = 0;
        for (byte i = 0; i < 3; ++i) {
            int delta = end[i] - start[i];
            int absDelta = Math.abs(delta);
            deltas[i] = delta;
            if (absDelta > Math.abs(deltas[mainAxis]))
                mainAxis = i;
        }
        if (deltas[mainAxis] != 0) {
            byte coordA = otherCoordPairs[mainAxis];
            byte coordB = otherCoordPairs[mainAxis + 3];
            byte stepDir = deltas[mainAxis] > 0 ? (byte) 1 : (byte) -1;
            float stepA = (float) deltas[coordA] / (float) deltas[mainAxis];
            float stepB = (float) deltas[coordB] / (float) deltas[mainAxis];
            int endVal = deltas[mainAxis] + stepDir;
            int[] current = deltas;
            for (int step = 0; step != endVal; step += stepDir) {
                current[mainAxis] = Mth.floor(start[mainAxis] + step + 0.5F);
                current[coordA] = Mth.floor(start[coordA] + step * stepA + 0.5F);
                current[coordB] = Mth.floor(start[coordB] + step * stepB + 0.5F);
                BlockState blockState = logState;
                int dx = Math.abs(current[0] - start[0]);
                int dz = Math.abs(current[2] - start[2]);
                int maxDist = Math.max(dx, dz);
                if (maxDist > 0 && (dx == maxDist || dz == maxDist)) {
                    blockState = woodState;
                }
                this.setBlockAndNotifyAdequately(world, current[0], current[1], current[2], blockState);
            }
        }
    }

    private void generateTrunk(BlockPos base) {
        int x = basePos[0];
        int y = basePos[1];
        int maxY = basePos[1] + height;
        int z = basePos[2];

        int[] bottomPoint = new int[]{x, y, z};
        int[] topPoint = new int[]{x, maxY, z};

        double lim = 400f / trunkSize;

        world.getChunk(base).setBlockState(base, Blocks.AIR.defaultBlockState(), 0);
        for (int i = -trunkSize; i <= trunkSize; i++) {
            bottomPoint[0] = x + i;
            topPoint[0] = x + i;
            for (int j = -trunkSize; j <= trunkSize; j++) {
                if ((j * j + i * i) * 4 < trunkSize * trunkSize * 5) {
                    bottomPoint[2] = z + j;
                    topPoint[2] = z + j;
                    if (slopeTrunk)
                        topPoint[1] = y + sinc2(lim * i, lim * j, height) - (rand.nextInt(3) - 1);
                    this.placeBlockLine(bottomPoint, topPoint, log, wood);
                    this.setBlockAndNotifyAdequately(world, topPoint[0], topPoint[1], topPoint[2], wood);
                }
            }
        }
    }

    private static int sinc2(final double x, final double z, final int y) {
        final double pi = Math.PI, pi2 = pi / 1.5;
        double r;
        r = Math.sqrt((r = (x / pi)) * r + (r = (z / pi)) * r) * pi / 180;
        if (r == 0) return y;
        return (int) Math.round(y * (((Math.sin(r) / r) + (Math.sin(r * pi2) / (r * pi2))) / 2));
    }

    private void generateLeafNodeBases() {
        int[] start = new int[]{basePos[0], basePos[1], basePos[2]};
        int[][] leafNodes = this.leafNodes;
        int heightLimit = (int) (this.heightLimit * 0.2f);
        for (int i = 0, e = leafNodesLength; i < e; ++i) {
            int[] end = leafNodes[i];
            start[1] = end[3];
            int height = start[1] - basePos[1];
            if (height >= heightLimit) {
                this.placeBlockLine(start, end, log, wood);
            }
        }
    }

    private int[] checkScratch = new int[3];

    private int checkBlockLine(int[] start, int[] end) {
        int[] deltas = checkScratch;
        byte mainAxis = 0;
        for (byte i = 0; i < 3; ++i) {
            int delta = end[i] - start[i];
            int absDelta = Math.abs(delta);
            deltas[i] = delta;
            if (absDelta > Math.abs(deltas[mainAxis]))
                mainAxis = i;
        }
        if (deltas[mainAxis] == 0)
            return -1;
        else {
            byte coordA = otherCoordPairs[mainAxis];
            byte coordB = otherCoordPairs[mainAxis + 3];
            byte stepDir = deltas[mainAxis] > 0 ? (byte) 1 : (byte) -1;
            float stepA = (float) deltas[coordA] / (float) deltas[mainAxis];
            float stepB = (float) deltas[coordB] / (float) deltas[mainAxis];
            int step = 0;
            int endVal = deltas[mainAxis] + stepDir;
            int[] current = deltas;
            for (; step != endVal; step += stepDir) {
                current[mainAxis] = start[mainAxis] + step;
                current[coordA] = Mth.floor(start[coordA] + step * stepA);
                current[coordB] = Mth.floor(start[coordB] + step * stepB);
                BlockPos pos = new BlockPos(current[0], current[1], current[2]);
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();
                if (safeGrowth ? !(canBeReplacedByLogs(state, world, pos) ||
                        block instanceof SaplingBlock) :
                        block == Blocks.BEDROCK)
                    break;
            }
            return step == endVal ? -1 : Math.abs(step);
        }
    }

    private boolean validTreeLocation() {
        int newHeight = Math.min(heightLimit + basePos[1], world.getHeight()) - basePos[1];
        if (newHeight < minHeight) {
            DEBUG_LOGGER.info("validTreeLocation FAIL at ({},{},{}): newHeight {} < minHeight {}",
                    basePos[0], basePos[1], basePos[2], newHeight, minHeight);
            return false;
        }
        heightLimit = newHeight;

        BlockPos pos = new BlockPos(basePos[0], basePos[1] - 1, basePos[2]);
        BlockState state = world.getBlockState(pos);

        if (!canSustainPlant(state, world, pos)) {
            DEBUG_LOGGER.info("validTreeLocation FAIL at ({},{},{}): block below is {}",
                    basePos[0], basePos[1], basePos[2], state);
            return false;
        }
        else {
            int[] checkStart = new int[]{basePos[0], basePos[1], basePos[2]};
            int[] checkEnd = new int[]{basePos[0], basePos[1] + heightLimit - 1, basePos[2]};
            newHeight = this.checkBlockLine(checkStart, checkEnd);
            if (newHeight == -1) newHeight = heightLimit;
            if (newHeight < minHeight)
                return false;
            heightLimit = Math.min(newHeight, heightLimitLimit);
            height = (int) (heightLimit * heightAttenuation);
            if (height >= heightLimit)
                height = heightLimit - 1;
            height += rand.nextInt(heightLimit - height);

            if (safeGrowth) {
                int x = basePos[0];
                int y = basePos[1];
                int topY = basePos[1] + height;
                int z = basePos[2];
                checkStart = new int[]{x, y, z};
                checkEnd = new int[]{x, topY, z};
                double lim = 400f / trunkSize;
                for (int i = -trunkSize; i <= trunkSize; i++) {
                    checkStart[0] = x + i;
                    checkEnd[0] = x + i;
                    for (int j = -trunkSize; j <= trunkSize; j++) {
                        if ((j * j + i * i) * 4 < trunkSize * trunkSize * 5) {
                            checkStart[2] = z + j;
                            checkEnd[2] = z + j;
                            if (slopeTrunk)
                                checkEnd[1] = y + sinc2(lim * i, lim * j, height);
                            int t = checkBlockLine(checkStart, checkEnd);
                            if (t != -1)
                                return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    public MassiveTreeGenerator setTreeScale(float height, float width, float leaves) {
        heightLimitLimit = (int) (height * 12.0D);
        minHeight = heightLimitLimit / 2;
        trunkSize = (int) Math.round((height / 2D));
        if (minHeight > 30)
            leafDistanceLimit = 5;
        else
            leafDistanceLimit = minHeight / 8;
        scaleWidth = width;
        branchDensity = leaves;
        return this;
    }

    public MassiveTreeGenerator setMinTrunkSize(int radius) {
        trunkSize = Math.max(radius, trunkSize);
        return this;
    }

    public MassiveTreeGenerator setLeafAttenuation(float attenuation) {
        heightAttenuation = attenuation;
        return this;
    }

    public MassiveTreeGenerator setSloped(boolean sloped) {
        slopeTrunk = sloped;
        return this;
    }

    public MassiveTreeGenerator setSafe(boolean safe) {
        safeGrowth = safe;
        return this;
    }

    public boolean generate(Level world, RandomSource random, BlockPos pos) {
        return generate(world, random.nextLong(), pos);
    }

    /**
     * Generate tree using a fixed seed (deterministic - same seed = same tree).
     * Used for batched/persistent tree generation.
     */
    public boolean generate(Level world, long seed, BlockPos pos) {
        this.world = world;
        rand = RandomSource.create(seed);
        basePos[0] = pos.getX();
        basePos[1] = pos.getY();
        basePos[2] = pos.getZ();
        if (heightLimit == 0)
            heightLimit = heightLimitLimit;
        if (minHeight == -1)
            minHeight = 80;

        if (!this.validTreeLocation()) {
            DEBUG_LOGGER.info("generate FAIL at ({},{},{}): validTreeLocation returned false",
                    pos.getX(), pos.getY(), pos.getZ());
            return false;
        }
        else {
            this.setup();
            this.generateLeafNodeList();
            this.generateLeaves();
            this.generateLeafNodeBases();
            this.generateTrunk(pos);
            this.updateChunks();
            return true;
        }
    }

    private final ArrayList<ChunkAccess> chunksToUpdate = new ArrayList<>();

    public void setBlockAndNotifyAdequately(Level world, int x, int y, int z, BlockState state) {
        // Skip blocks outside world build height limits
        if (y < world.getMinY() || y >= world.getMaxY()) return;
        BlockPos pos = new BlockPos(x, y, z);

        // In collect mode: record placement for later batched execution
        if (collectMode) {
            if (safeGrowth && !canBeReplacedByLogs(world.getBlockState(pos), world, pos)) return;
            collectedPlacements.add(pos.asLong(), state);
            return;
        }

        // Normal mode: place immediately
        if (safeGrowth && !canBeReplacedByLogs(world.getBlockState(pos), world, pos)) return;
        ChunkAccess chunk = world.getChunk(pos);
        chunk.setBlockState(pos, state, 0);
        if (world instanceof ServerLevel) {
            ((ServerLevel) world).getChunkSource().blockChanged(pos);
        }
        if (!chunksToUpdate.contains(chunk)) chunksToUpdate.add(chunk);
    }

    public void updateChunks() {
        // MC 26.1 handles lighting automatically after block changes.
        // Light engine manual updates are no longer needed.
    }

}
