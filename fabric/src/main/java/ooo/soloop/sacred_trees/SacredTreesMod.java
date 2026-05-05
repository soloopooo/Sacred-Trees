package ooo.soloop.sacred_trees;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SacredTreesMod implements ModInitializer {
    public static final String MODID = "sacred_trees";
    public static final Logger LOGGER = LoggerFactory.getLogger(SacredTreesMod.class);

    @Override
    public void onInitialize() {
        // Register all blocks and items
        ModBlocks.registerAll();
        ModItems.registerAll();
        // Register creative tab content
        ModCreativeTab.register();

        // Initialize platform hooks
        FabricPlatformHooksImpl.init();

        // Resume pending tree generation on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                TreePlacementTask.resumePending(level);
            }
        });

        LOGGER.info("Sacred Trees loaded!");
    }
}
