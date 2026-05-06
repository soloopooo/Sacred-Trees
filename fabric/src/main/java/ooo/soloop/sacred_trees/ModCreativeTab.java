package ooo.soloop.sacred_trees;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTab {
    private static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(SacredTreesMod.MODID, "sacred_trees"));

    public static void register() {
        // Register creative mode tab using Fabric API
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                TAB_KEY,
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.sacred_trees"))
                        .icon(() -> new ItemStack(ModBlocks.OAK.MASSIVE))
                        .build());

        // Add items to the custom tab via Fabric's event system
        CreativeModeTabEvents.modifyOutputEvent(TAB_KEY).register(output -> {
            addAllItems(output);
        });
    }

    private static void addAllItems(CreativeModeTab.Output output) {
        output.accept(new ItemStack(ModBlocks.OAK.SACRED));
        output.accept(new ItemStack(ModBlocks.OAK.MEGA));
        output.accept(new ItemStack(ModBlocks.OAK.MASSIVE));
        output.accept(new ItemStack(ModBlocks.BIRCH.SACRED));
        output.accept(new ItemStack(ModBlocks.BIRCH.MEGA));
        output.accept(new ItemStack(ModBlocks.BIRCH.MASSIVE));
        output.accept(new ItemStack(ModBlocks.SPRUCE.SACRED));
        output.accept(new ItemStack(ModBlocks.SPRUCE.MEGA));
        output.accept(new ItemStack(ModBlocks.SPRUCE.MASSIVE));
        output.accept(new ItemStack(ModBlocks.JUNGLE.SACRED));
        output.accept(new ItemStack(ModBlocks.JUNGLE.MEGA));
        output.accept(new ItemStack(ModBlocks.JUNGLE.MASSIVE));
        output.accept(new ItemStack(ModBlocks.ACACIA.SACRED));
        output.accept(new ItemStack(ModBlocks.ACACIA.MEGA));
        output.accept(new ItemStack(ModBlocks.ACACIA.MASSIVE));
        output.accept(new ItemStack(ModBlocks.DARK_OAK.SACRED));
        output.accept(new ItemStack(ModBlocks.DARK_OAK.MEGA));
        output.accept(new ItemStack(ModBlocks.DARK_OAK.MASSIVE));
        output.accept(new ItemStack(ModBlocks.CHERRY.SACRED));
        output.accept(new ItemStack(ModBlocks.CHERRY.MEGA));
        output.accept(new ItemStack(ModBlocks.CHERRY.MASSIVE));
        output.accept(new ItemStack(ModBlocks.MANGROVE.SACRED));
        output.accept(new ItemStack(ModBlocks.MANGROVE.MEGA));
        output.accept(new ItemStack(ModBlocks.MANGROVE.MASSIVE));
        output.accept(new ItemStack(ModBlocks.PALE_OAK.SACRED));
        output.accept(new ItemStack(ModBlocks.PALE_OAK.MEGA));
        output.accept(new ItemStack(ModBlocks.PALE_OAK.MASSIVE));
        output.accept(new ItemStack(ModBlocks.CRIMSON.SACRED));
        output.accept(new ItemStack(ModBlocks.CRIMSON.MEGA));
        output.accept(new ItemStack(ModBlocks.CRIMSON.MASSIVE));
        output.accept(new ItemStack(ModBlocks.WARPED.SACRED));
        output.accept(new ItemStack(ModBlocks.WARPED.MEGA));
        output.accept(new ItemStack(ModBlocks.WARPED.MASSIVE));
    }
}
