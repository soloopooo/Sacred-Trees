package ooo.soloop.sacred_trees;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

public class ModItems {

    public static void registerAll() {
        // Register BlockItems for all blocks
        registerBlockItem("sacred_oak_sapling", ModBlocks.OAK.SACRED, Rarity.RARE);
        registerBlockItem("mega_oak_sapling", ModBlocks.OAK.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_oak_sapling", ModBlocks.OAK.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_birch_sapling", ModBlocks.BIRCH.SACRED, Rarity.RARE);
        registerBlockItem("mega_birch_sapling", ModBlocks.BIRCH.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_birch_sapling", ModBlocks.BIRCH.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_spruce_sapling", ModBlocks.SPRUCE.SACRED, Rarity.RARE);
        registerBlockItem("mega_spruce_sapling", ModBlocks.SPRUCE.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_spruce_sapling", ModBlocks.SPRUCE.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_jungle_sapling", ModBlocks.JUNGLE.SACRED, Rarity.RARE);
        registerBlockItem("mega_jungle_sapling", ModBlocks.JUNGLE.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_jungle_sapling", ModBlocks.JUNGLE.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_acacia_sapling", ModBlocks.ACACIA.SACRED, Rarity.RARE);
        registerBlockItem("mega_acacia_sapling", ModBlocks.ACACIA.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_acacia_sapling", ModBlocks.ACACIA.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_dark_oak_sapling", ModBlocks.DARK_OAK.SACRED, Rarity.RARE);
        registerBlockItem("mega_dark_oak_sapling", ModBlocks.DARK_OAK.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_dark_oak_sapling", ModBlocks.DARK_OAK.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_crimson_fungus", ModBlocks.CRIMSON.SACRED, Rarity.RARE);
        registerBlockItem("mega_crimson_fungus", ModBlocks.CRIMSON.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_crimson_fungus", ModBlocks.CRIMSON.MASSIVE, Rarity.EPIC, true);

        registerBlockItem("sacred_warped_fungus", ModBlocks.WARPED.SACRED, Rarity.RARE);
        registerBlockItem("mega_warped_fungus", ModBlocks.WARPED.MEGA, Rarity.UNCOMMON);
        registerBlockItem("massive_warped_fungus", ModBlocks.WARPED.MASSIVE, Rarity.EPIC, true);
    }

    private static void registerBlockItem(String name, Block block, Rarity rarity) {
        registerBlockItem(name, block, rarity, false);
    }

    private static void registerBlockItem(String name, Block block, Rarity rarity, boolean foiled) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SacredTreesMod.MODID, name));
        Item.Properties props = new Item.Properties()
                .rarity(rarity)
                .setId(key)
                .useBlockDescriptionPrefix();
        Item item;
        if (foiled) {
            item = new FoiledBlockItem(block, props);
        } else {
            item = new BlockItem(block, props);
        }
        Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
