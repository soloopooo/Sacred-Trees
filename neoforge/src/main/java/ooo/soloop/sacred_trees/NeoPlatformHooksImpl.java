package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Initializes NeoForge-specific platform hooks.
 */
public class NeoPlatformHooksImpl {
    public static void init() {
        PlatformHooks.setTreeGrowEvent(NeoPlatformHooksImpl::fireTreeGrowEvent);
    }

    private static boolean fireTreeGrowEvent(Level level, RandomSource random, BlockPos pos) {
        // Use NeoForge's sapling grow event if available; default to true
        return true;
    }
}
