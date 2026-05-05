package ooo.soloop.sacred_trees.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Client setup for Fabric.
 * Render layers are determined by block model JSON files,
 * so no explicit registration is needed.
 */
public class FabricClientSetup implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Render types are automatically handled via block model JSON (render_type field).
        // No explicit BlockRenderLayerMap registration needed.
    }
}
