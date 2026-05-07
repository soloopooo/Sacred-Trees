package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tree generator for mangrove trees.
 *
 * Uses a two-pass approach matching vanilla's AttachedToLeavesDecorator:
 * 1. Generate the tree structure (trunk, leaves) as normal, but record
 *    leaf positions and pre-compute propagule states (to preserve the
 *    exact random consumption order for determinism).
 * 2. In {@link #generate}, after the tree structure is complete, append
 *    all pending propagule entries to the placement list in one batch.
 *
 * This separates leaf generation from propagule placement, keeping the
 * collect list cleanly ordered: all tree blocks first, then propagules.
 */
public class MangroveTreeGenerator extends MassiveTreeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("TreeGenDebug");
    private static final float PROPAGULE_CHANCE = 0.15f;
    private static final int PROPAGULE_OFFSET = -1;

    /** Pre-computed propagule entries, built during tree generation. */
    private final PlacementBuffer pendingPropagules = new PlacementBuffer();

    public MangroveTreeGenerator(BlockState log, BlockState wood, BlockState leaves) {
        super(log, wood, leaves);
    }

    @Override
    public boolean generate(Level world, long seed, BlockPos pos) {
        pendingPropagules.clear();
        // Pass 1: generate tree structure. Propagule decisions are pre-computed
        // in setBlockAndNotifyAdequately to preserve random consumption order.
        boolean result = super.generate(world, seed, pos);
        if (!result) return false;

        // Pass 2: append all pending propagules into the placement list
        // in a single batch, after all tree blocks.
        int count = 0;
        for (int i = 0; i < pendingPropagules.size(); i++) {
            BlockPos p = BlockPos.of(pendingPropagules.getPos(i));
            super.setBlockAndNotifyAdequately(world, p.getX(), p.getY(), p.getZ(), pendingPropagules.getState(i));
            count++;
        }
        if (count > 0) {
            LOGGER.info("MangroveTreeGenerator: added {} hanging propagules at {}", count, pos);
        }
        return true;
    }

    @Override
    public void setBlockAndNotifyAdequately(Level world, int x, int y, int z, BlockState state) {
        // When placing a leaf, pre-compute the propagule decision now to
        // preserve random consumption order, but store it instead of placing.
        // Actual placement happens in generate() as a second pass.
        if (state.getBlock() == leaves.getBlock() && rand.nextFloat() < PROPAGULE_CHANCE) {
            int age = rand.nextInt(4);
            BlockState propaguleState = Blocks.MANGROVE_PROPAGULE.defaultBlockState()
                    .setValue(MangrovePropaguleBlock.HANGING, true)
                    .setValue(MangrovePropaguleBlock.AGE, age)
                    .setValue(BlockStateProperties.WATERLOGGED, false);
            pendingPropagules.add(BlockPos.asLong(x, y + PROPAGULE_OFFSET, z), propaguleState);
        }
        super.setBlockAndNotifyAdequately(world, x, y, z, state);
    }
}
