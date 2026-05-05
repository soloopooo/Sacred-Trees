package ooo.soloop.sacred_trees;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Client-side setup for NeoForge.
 * Render types are handled via the model system in NeoForge.
 */
@EventBusSubscriber(modid = SacredTreesMod.MODID, value = Dist.CLIENT)
public class NeoClientSetup {
    // Render types determined by model JSON - no explicit registration needed
}
