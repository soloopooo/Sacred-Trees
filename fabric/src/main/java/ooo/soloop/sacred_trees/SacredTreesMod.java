package ooo.soloop.sacred_trees;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

        // Load config
        SacredTreesConfig.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());

        // Initialize platform hooks
        FabricPlatformHooksImpl.init();

        // Resume pending tree generation on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                TreePlacementTask.resumePending(level);
            }
        });

        // Process one batch of tree placement per server tick
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            TreePlacementTask.onServerTick();
        });

        LOGGER.info("Sacred Trees loaded!");
    }
}
