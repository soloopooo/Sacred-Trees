package io.bluebeaker.sacredtrees;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SacredTreesMod.MODID)
public class SacredTreesMod {
    public static final String MODID = "sacred_trees";
    public static final Logger LOGGER = LoggerFactory.getLogger(SacredTreesMod.class);

    public SacredTreesMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register deferred registers
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

        // Setup common
        modEventBus.addListener(this::commonSetup);

        // Register platform hooks
        NeoPlatformHooksImpl.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Sacred Trees loaded!");
    }
}
