package ooo.soloop.sacred_trees;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Platform abstraction hooks.
 * Each platform module provides its own implementation
 * to handle platform-specific events (e.g., tree grow events).
 */
public class PlatformHooks {
    private static TreeGrowEvent treeGrowEvent;

    /**
     * Set the tree grow event handler for the current platform.
     */
    public static void setTreeGrowEvent(TreeGrowEvent event) {
        treeGrowEvent = event;
    }

    /**
     * Fire the tree grow event. Returns false if the event was cancelled.
     */
    public static boolean fireTreeGrowEvent(Level level, RandomSource random, BlockPos pos) {
        if (treeGrowEvent != null) {
            return treeGrowEvent.fireTreeGrowEvent(level, random, pos);
        }
        return true;
    }

    public interface TreeGrowEvent {
        boolean fireTreeGrowEvent(Level level, RandomSource random, BlockPos pos);
    }
}
