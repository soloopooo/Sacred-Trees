package ooo.soloop.sacred_trees;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SacredTreesMod.MODID);

    // === Oak ===
    public static final DeferredItem<Item> SACRED_OAK = blockItem("sacred_oak_sapling", ModBlocks.OAK.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_OAK = blockItem("mega_oak_sapling", ModBlocks.OAK.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_OAK = foiledBlockItem("massive_oak_sapling", ModBlocks.OAK.MASSIVE, Rarity.EPIC);

    // === Birch ===
    public static final DeferredItem<Item> SACRED_BIRCH = blockItem("sacred_birch_sapling", ModBlocks.BIRCH.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_BIRCH = blockItem("mega_birch_sapling", ModBlocks.BIRCH.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_BIRCH = foiledBlockItem("massive_birch_sapling", ModBlocks.BIRCH.MASSIVE, Rarity.EPIC);

    // === Spruce ===
    public static final DeferredItem<Item> SACRED_SPRUCE = blockItem("sacred_spruce_sapling", ModBlocks.SPRUCE.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_SPRUCE = blockItem("mega_spruce_sapling", ModBlocks.SPRUCE.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_SPRUCE = foiledBlockItem("massive_spruce_sapling", ModBlocks.SPRUCE.MASSIVE, Rarity.EPIC);

    // === Jungle ===
    public static final DeferredItem<Item> SACRED_JUNGLE = blockItem("sacred_jungle_sapling", ModBlocks.JUNGLE.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_JUNGLE = blockItem("mega_jungle_sapling", ModBlocks.JUNGLE.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_JUNGLE = foiledBlockItem("massive_jungle_sapling", ModBlocks.JUNGLE.MASSIVE, Rarity.EPIC);

    // === Acacia ===
    public static final DeferredItem<Item> SACRED_ACACIA = blockItem("sacred_acacia_sapling", ModBlocks.ACACIA.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_ACACIA = blockItem("mega_acacia_sapling", ModBlocks.ACACIA.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_ACACIA = foiledBlockItem("massive_acacia_sapling", ModBlocks.ACACIA.MASSIVE, Rarity.EPIC);

    // === Dark Oak ===
    public static final DeferredItem<Item> SACRED_DARK_OAK = blockItem("sacred_dark_oak_sapling", ModBlocks.DARK_OAK.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_DARK_OAK = blockItem("mega_dark_oak_sapling", ModBlocks.DARK_OAK.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_DARK_OAK = foiledBlockItem("massive_dark_oak_sapling", ModBlocks.DARK_OAK.MASSIVE, Rarity.EPIC);

    // === Cherry ===
    public static final DeferredItem<Item> SACRED_CHERRY = blockItem("sacred_cherry_sapling", ModBlocks.CHERRY.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_CHERRY = blockItem("mega_cherry_sapling", ModBlocks.CHERRY.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_CHERRY = foiledBlockItem("massive_cherry_sapling", ModBlocks.CHERRY.MASSIVE, Rarity.EPIC);

    // === Mangrove ===
    public static final DeferredItem<Item> SACRED_MANGROVE = blockItem("sacred_mangrove_sapling", ModBlocks.MANGROVE.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_MANGROVE = blockItem("mega_mangrove_sapling", ModBlocks.MANGROVE.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_MANGROVE = foiledBlockItem("massive_mangrove_sapling", ModBlocks.MANGROVE.MASSIVE, Rarity.EPIC);

    // === Pale Oak ===
    public static final DeferredItem<Item> SACRED_PALE_OAK = blockItem("sacred_pale_oak_sapling", ModBlocks.PALE_OAK.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_PALE_OAK = blockItem("mega_pale_oak_sapling", ModBlocks.PALE_OAK.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_PALE_OAK = foiledBlockItem("massive_pale_oak_sapling", ModBlocks.PALE_OAK.MASSIVE, Rarity.EPIC);

    // === Crimson ===
    public static final DeferredItem<Item> SACRED_CRIMSON = blockItem("sacred_crimson_fungus", ModBlocks.CRIMSON.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_CRIMSON = blockItem("mega_crimson_fungus", ModBlocks.CRIMSON.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_CRIMSON = foiledBlockItem("massive_crimson_fungus", ModBlocks.CRIMSON.MASSIVE, Rarity.EPIC);

    // === Warped ===
    public static final DeferredItem<Item> SACRED_WARPED = blockItem("sacred_warped_fungus", ModBlocks.WARPED.SACRED, Rarity.RARE);
    public static final DeferredItem<Item> MEGA_WARPED = blockItem("mega_warped_fungus", ModBlocks.WARPED.MEGA, Rarity.UNCOMMON);
    public static final DeferredItem<Item> MASSIVE_WARPED = foiledBlockItem("massive_warped_fungus", ModBlocks.WARPED.MASSIVE, Rarity.EPIC);

    /** Register a BlockItem with block description prefix. */
    private static DeferredItem<Item> blockItem(String name, DeferredBlock<?> block, Rarity rarity) {
        return ITEMS.registerItem(name, props -> new BlockItem(block.get(),
                props.useBlockDescriptionPrefix().rarity(rarity)));
    }

    /** Register a foiled (enchanted glint) BlockItem. */
    private static DeferredItem<Item> foiledBlockItem(String name, DeferredBlock<?> block, Rarity rarity) {
        return ITEMS.registerItem(name, props -> new FoiledBlockItem(block.get(),
                props.useBlockDescriptionPrefix().rarity(rarity)));
    }
}
