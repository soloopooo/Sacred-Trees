package io.bluebeaker.sacredtrees;

import net.fabricmc.api.ModInitializer;
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

        // Initialize platform hooks (Fabric has no sapling grow event, so default is fine)
        FabricPlatformHooksImpl.init();

        LOGGER.info("Sacred Trees loaded!");
    }
}
