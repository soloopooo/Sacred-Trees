package io.bluebeaker.sacredtrees;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    // Sapling variants
    public static final SaplingVariants OAK = registerSaplingTypes("oak_sapling",
            Blocks.OAK_LOG, Blocks.OAK_WOOD, Blocks.OAK_LEAVES);
    public static final SaplingVariants BIRCH = registerSaplingTypes("birch_sapling",
            Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD, Blocks.BIRCH_LEAVES);
    public static final SaplingVariants SPRUCE = registerSaplingTypes("spruce_sapling",
            Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LEAVES);
    public static final SaplingVariants JUNGLE = registerSaplingTypes("jungle_sapling",
            Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LEAVES);
    public static final SaplingVariants ACACIA = registerSaplingTypes("acacia_sapling",
            Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD, Blocks.ACACIA_LEAVES);
    public static final SaplingVariants DARK_OAK = registerSaplingTypes("dark_oak_sapling",
            Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LEAVES);

    // Fungus variants
    public static final SaplingVariants CRIMSON = registerFungusTypes("crimson_fungus",
            Blocks.CRIMSON_STEM, Blocks.CRIMSON_HYPHAE, Blocks.NETHER_WART_BLOCK,
            Blocks.SHROOMLIGHT, Blocks.WEEPING_VINES_PLANT, Blocks.WEEPING_VINES);
    public static final SaplingVariants WARPED = registerFungusTypes("warped_fungus",
            Blocks.WARPED_STEM, Blocks.WARPED_HYPHAE, Blocks.WARPED_WART_BLOCK,
            Blocks.SHROOMLIGHT, null, null);

    public static void registerAll() {
        // Blocks are registered in static initializers via registerBlock calls
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SacredTreesMod.MODID, name));
    }

    private static BlockBehaviour.Properties keyedProperties(String name) {
        return BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.PLANT)
                .noCollision()
                .instabreak()
                .noOcclusion()
                .sound(SoundType.GRASS);
    }

    private static BlockBehaviour.Properties keyedFungusProperties(String name) {
        return BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.PLANT)
                .noCollision()
                .instabreak()
                .randomTicks()
                .noOcclusion()
                .sound(SoundType.GRASS);
    }

    private static SaplingVariants registerSaplingTypes(String basename, Block log, Block wood, Block leaves) {
        return new SaplingVariants(
                registerBlock("sacred_" + basename,
                        new FabricSacredSapling(log, wood, leaves, keyedProperties("sacred_" + basename), AbstractSacredSapling.Type.SACRED_SPRING)),
                registerBlock("mega_" + basename,
                        new FabricSacredSapling(log, wood, leaves, keyedProperties("mega_" + basename), AbstractSacredSapling.Type.MEGA)),
                registerBlock("massive_" + basename,
                        new FabricSacredSapling(log, wood, leaves, keyedProperties("massive_" + basename), AbstractSacredSapling.Type.MASSIVE))
        );
    }

    private static SaplingVariants registerFungusTypes(String basename, Block log, Block wood, Block leaves,
                                                        Block lights, Block vines, Block vines2) {
        return new SaplingVariants(
                registerBlock("sacred_" + basename,
                        new FabricSacredFungus(log, wood, leaves, lights, vines, vines2, keyedFungusProperties("sacred_" + basename), AbstractSacredSapling.Type.SACRED_SPRING)),
                registerBlock("mega_" + basename,
                        new FabricSacredFungus(log, wood, leaves, lights, vines, vines2, keyedFungusProperties("mega_" + basename), AbstractSacredSapling.Type.MEGA)),
                registerBlock("massive_" + basename,
                        new FabricSacredFungus(log, wood, leaves, lights, vines, vines2, keyedFungusProperties("massive_" + basename), AbstractSacredSapling.Type.MASSIVE))
        );
    }

    private static Block registerBlock(String name, Block block) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(SacredTreesMod.MODID, name));
        Registry.register(BuiltInRegistries.BLOCK, key, block);
        return block;
    }

    public static class SaplingVariants {
        public final Block SACRED;
        public final Block MEGA;
        public final Block MASSIVE;

        public SaplingVariants(Block sacred, Block mega, Block massive) {
            this.SACRED = sacred;
            this.MEGA = mega;
            this.MASSIVE = massive;
        }
    }
}
