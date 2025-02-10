package com.tabslab.tabsmod.init;

import com.tabslab.tabsmod.TabsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid= TabsMod.MODID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTab {

    @SubscribeEvent
    public static void onCreativeTabRegistry(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(TabsMod.MODID, "tabslab"), builder -> {
            builder
                    .title(Component.literal("tabslab"))
                    .icon(() -> new ItemStack(BlockInit.BLOCK_A.get()))
                    .displayItems((enabledFlags, populator, hasPermissions) -> {
                        populator.accept(new ItemStack(BlockInit.BLOCK_A.get()));
                        populator.accept(new ItemStack(BlockInit.BLOCK_B.get()));
                        populator.accept(new ItemStack(BlockInit.TABS_FENCE.get()));
                        populator.accept(new ItemStack(ItemInit.COIN.get()));
                    });
        });
    }

}
