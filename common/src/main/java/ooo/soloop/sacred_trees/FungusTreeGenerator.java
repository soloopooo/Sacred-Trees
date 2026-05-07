package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tree generator for fungus (crimson, warped) trees.
 *
 * Uses a two-pass approach to avoid interleaving decoration blocks
 * (shroomlights, weeping vines) with leaf placement:
 * 1. Generate the tree structure (trunk, leaves) as normal — decorations
 *    are detected and recorded, but NOT placed during this pass.
 * 2. In {@link #generate}, after the tree structure is complete, apply
 *    all recorded decorations in a single batch.
 *
 * This prevents the rendering lag caused by interleaved block placement,
 * matching the fix applied to MangroveTreeGenerator.
 */
public class FungusTreeGenerator extends MassiveTreeGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger("TreeGenDebug");

    /** Pre-computed decoration entries (lights, vines), built during tree gen. */
    private final PlacementBuffer pendingDecorations = new PlacementBuffer();

    public FungusTreeGenerator(BlockState log, BlockState wood, BlockState leaves,
                               BlockState lights, BlockState vines, BlockState vines2) {
        super(log, wood, leaves, lights, vines, vines2);
    }

    @Override
    public boolean generate(Level world, long seed, BlockPos pos) {
        pendingDecorations.clear();
        // Pass 1: generate tree structure. Decorations are detected and recorded
        // in setBlockAndNotifyAdequately, but NOT placed yet.
        boolean result = super.generate(world, seed, pos);
        if (!result) return false;

        // Pass 2: apply all recorded decorations (lights overwrite leaves,
        // vines hang below) in a single batch after all tree blocks.
        int count = 0;
        for (int i = 0; i < pendingDecorations.size(); i++) {
            BlockPos p = BlockPos.of(pendingDecorations.getPos(i));
            super.setBlockAndNotifyAdequately(world, p.getX(), p.getY(), p.getZ(), pendingDecorations.getState(i));
            count++;
        }
        if (count > 0) {
            LOGGER.info("FungusTreeGenerator: added {} decorations at {}", count, pos);
        }
        return true;
    }

    @Override
    public void setBlockAndNotifyAdequately(Level world, int x, int y, int z, BlockState state) {
        if (genLightsAndVines) {
            // Light block: the original code replaces a leaf with a light at
            // the same position. We place the leaf normally in Pass 1 and
            // record the light to overwrite it in Pass 2.
            if (state == lights) {
                pendingDecorations.add(BlockPos.asLong(x, y, z), state);
                super.setBlockAndNotifyAdequately(world, x, y, z, persistentLeaves);
                return;
            }
            // Vine blocks: skip placement in Pass 1 entirely — record for Pass 2.
            if (state == vines || state == vines2) {
                pendingDecorations.add(BlockPos.asLong(x, y, z), state);
                return;
            }
        }
        super.setBlockAndNotifyAdequately(world, x, y, z, state);
    }
}
