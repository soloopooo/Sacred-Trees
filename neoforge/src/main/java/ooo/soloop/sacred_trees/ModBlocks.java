package ooo.soloop.sacred_trees;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SacredTreesMod.MODID);

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

    private static SaplingVariants registerSaplingTypes(String basename, Block log, Block wood, Block leaves) {
        return new SaplingVariants(
                BLOCKS.registerBlock("sacred_" + basename,
                        p -> new NeoSacredSapling(log, wood, leaves, p, AbstractSacredSapling.Type.SACRED_SPRING),
                        BlockBehaviour.Properties::of),
                BLOCKS.registerBlock("mega_" + basename,
                        p -> new NeoSacredSapling(log, wood, leaves, p, AbstractSacredSapling.Type.MEGA),
                        BlockBehaviour.Properties::of),
                BLOCKS.registerBlock("massive_" + basename,
                        p -> new NeoSacredSapling(log, wood, leaves, p, AbstractSacredSapling.Type.MASSIVE),
                        BlockBehaviour.Properties::of)
        );
    }

    private static SaplingVariants registerFungusTypes(String basename, Block log, Block wood, Block leaves,
                                                        Block lights, Block vines, Block vines2) {
        return new SaplingVariants(
                BLOCKS.registerBlock("sacred_" + basename,
                        p -> new NeoSacredFungus(log, wood, leaves, lights, vines, vines2, p, AbstractSacredSapling.Type.SACRED_SPRING),
                        BlockBehaviour.Properties::of),
                BLOCKS.registerBlock("mega_" + basename,
                        p -> new NeoSacredFungus(log, wood, leaves, lights, vines, vines2, p, AbstractSacredSapling.Type.MEGA),
                        BlockBehaviour.Properties::of),
                BLOCKS.registerBlock("massive_" + basename,
                        p -> new NeoSacredFungus(log, wood, leaves, lights, vines, vines2, p, AbstractSacredSapling.Type.MASSIVE),
                        BlockBehaviour.Properties::of)
        );
    }

    public static class SaplingVariants {
        public final DeferredBlock<? extends Block> SACRED;
        public final DeferredBlock<? extends Block> MEGA;
        public final DeferredBlock<? extends Block> MASSIVE;

        public SaplingVariants(DeferredBlock<? extends Block> sacred,
                               DeferredBlock<? extends Block> mega,
                               DeferredBlock<? extends Block> massive) {
            this.SACRED = sacred;
            this.MEGA = mega;
            this.MASSIVE = massive;
        }
    }
}
