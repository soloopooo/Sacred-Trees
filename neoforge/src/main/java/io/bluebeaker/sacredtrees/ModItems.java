package io.bluebeaker.sacredtrees;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SacredTreesMod.MODID);

    // === Oak ===
    public static final DeferredItem<Item> SACRED_OAK = ITEMS.registerItem("sacred_oak_sapling", props -> new BlockItem(ModBlocks.OAK.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_OAK = ITEMS.registerItem("mega_oak_sapling", props -> new BlockItem(ModBlocks.OAK.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_OAK = ITEMS.registerItem("massive_oak_sapling", props -> new FoiledBlockItem(ModBlocks.OAK.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Birch ===
    public static final DeferredItem<Item> SACRED_BIRCH = ITEMS.registerItem("sacred_birch_sapling", props -> new BlockItem(ModBlocks.BIRCH.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_BIRCH = ITEMS.registerItem("mega_birch_sapling", props -> new BlockItem(ModBlocks.BIRCH.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_BIRCH = ITEMS.registerItem("massive_birch_sapling", props -> new FoiledBlockItem(ModBlocks.BIRCH.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Spruce ===
    public static final DeferredItem<Item> SACRED_SPRUCE = ITEMS.registerItem("sacred_spruce_sapling", props -> new BlockItem(ModBlocks.SPRUCE.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_SPRUCE = ITEMS.registerItem("mega_spruce_sapling", props -> new BlockItem(ModBlocks.SPRUCE.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_SPRUCE = ITEMS.registerItem("massive_spruce_sapling", props -> new FoiledBlockItem(ModBlocks.SPRUCE.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Jungle ===
    public static final DeferredItem<Item> SACRED_JUNGLE = ITEMS.registerItem("sacred_jungle_sapling", props -> new BlockItem(ModBlocks.JUNGLE.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_JUNGLE = ITEMS.registerItem("mega_jungle_sapling", props -> new BlockItem(ModBlocks.JUNGLE.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_JUNGLE = ITEMS.registerItem("massive_jungle_sapling", props -> new FoiledBlockItem(ModBlocks.JUNGLE.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Acacia ===
    public static final DeferredItem<Item> SACRED_ACACIA = ITEMS.registerItem("sacred_acacia_sapling", props -> new BlockItem(ModBlocks.ACACIA.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_ACACIA = ITEMS.registerItem("mega_acacia_sapling", props -> new BlockItem(ModBlocks.ACACIA.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_ACACIA = ITEMS.registerItem("massive_acacia_sapling", props -> new FoiledBlockItem(ModBlocks.ACACIA.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Dark Oak ===
    public static final DeferredItem<Item> SACRED_DARK_OAK = ITEMS.registerItem("sacred_dark_oak_sapling", props -> new BlockItem(ModBlocks.DARK_OAK.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_DARK_OAK = ITEMS.registerItem("mega_dark_oak_sapling", props -> new BlockItem(ModBlocks.DARK_OAK.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_DARK_OAK = ITEMS.registerItem("massive_dark_oak_sapling", props -> new FoiledBlockItem(ModBlocks.DARK_OAK.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Crimson ===
    public static final DeferredItem<Item> SACRED_CRIMSON = ITEMS.registerItem("sacred_crimson_fungus", props -> new BlockItem(ModBlocks.CRIMSON.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_CRIMSON = ITEMS.registerItem("mega_crimson_fungus", props -> new BlockItem(ModBlocks.CRIMSON.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_CRIMSON = ITEMS.registerItem("massive_crimson_fungus", props -> new FoiledBlockItem(ModBlocks.CRIMSON.MASSIVE.get(), props.rarity(Rarity.EPIC)));

    // === Warped ===
    public static final DeferredItem<Item> SACRED_WARPED = ITEMS.registerItem("sacred_warped_fungus", props -> new BlockItem(ModBlocks.WARPED.SACRED.get(), props.rarity(Rarity.RARE)));
    public static final DeferredItem<Item> MEGA_WARPED = ITEMS.registerItem("mega_warped_fungus", props -> new BlockItem(ModBlocks.WARPED.MEGA.get(), props.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MASSIVE_WARPED = ITEMS.registerItem("massive_warped_fungus", props -> new FoiledBlockItem(ModBlocks.WARPED.MASSIVE.get(), props.rarity(Rarity.EPIC)));
}
