package io.bluebeaker.sacredtrees;

/**
 * Fabric platform hooks - no sapling grow event, so default behavior (return true) is used.
 */
public class FabricPlatformHooksImpl {
    public static void init() {
        // Fabric does not have a sapling grow event like NeoForge.
        // PlatformHooks.fireTreeGrowEvent() will return true by default.
    }
}
