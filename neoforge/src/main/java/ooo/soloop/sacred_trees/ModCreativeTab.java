package ooo.soloop.sacred_trees;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SacredTreesMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SACRED_TREES_TAB =
            CREATIVE_MODE_TABS.register("sacred_trees", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.sacred_trees"))
                    .icon(() -> new ItemStack(ModBlocks.OAK.MASSIVE.get()))
                    .displayItems((parameters, output) -> {
                        // Add all items to the tab
                        output.accept(ModItems.SACRED_OAK.get());
                        output.accept(ModItems.MEGA_OAK.get());
                        output.accept(ModItems.MASSIVE_OAK.get());
                        output.accept(ModItems.SACRED_BIRCH.get());
                        output.accept(ModItems.MEGA_BIRCH.get());
                        output.accept(ModItems.MASSIVE_BIRCH.get());
                        output.accept(ModItems.SACRED_SPRUCE.get());
                        output.accept(ModItems.MEGA_SPRUCE.get());
                        output.accept(ModItems.MASSIVE_SPRUCE.get());
                        output.accept(ModItems.SACRED_JUNGLE.get());
                        output.accept(ModItems.MEGA_JUNGLE.get());
                        output.accept(ModItems.MASSIVE_JUNGLE.get());
                        output.accept(ModItems.SACRED_ACACIA.get());
                        output.accept(ModItems.MEGA_ACACIA.get());
                        output.accept(ModItems.MASSIVE_ACACIA.get());
                        output.accept(ModItems.SACRED_DARK_OAK.get());
                        output.accept(ModItems.MEGA_DARK_OAK.get());
                        output.accept(ModItems.MASSIVE_DARK_OAK.get());
                        output.accept(ModItems.SACRED_CHERRY.get());
                        output.accept(ModItems.MEGA_CHERRY.get());
                        output.accept(ModItems.MASSIVE_CHERRY.get());
                        output.accept(ModItems.SACRED_MANGROVE.get());
                        output.accept(ModItems.MEGA_MANGROVE.get());
                        output.accept(ModItems.MASSIVE_MANGROVE.get());
                        output.accept(ModItems.SACRED_PALE_OAK.get());
                        output.accept(ModItems.MEGA_PALE_OAK.get());
                        output.accept(ModItems.MASSIVE_PALE_OAK.get());
                        output.accept(ModItems.SACRED_CRIMSON.get());
                        output.accept(ModItems.MEGA_CRIMSON.get());
                        output.accept(ModItems.MASSIVE_CRIMSON.get());
                        output.accept(ModItems.SACRED_WARPED.get());
                        output.accept(ModItems.MEGA_WARPED.get());
                        output.accept(ModItems.MASSIVE_WARPED.get());
                    })
                    .build());
}
