package com.tabslab.tabsmod.init;

import com.tabslab.tabsmod.TabsMod;
import com.tabslab.tabsmod.blocks.BlockA;
import com.tabslab.tabsmod.blocks.BlockB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid=TabsMod.MODID)
public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TabsMod.MODID);

    // Define custom block behaviors
    private static final BlockBehaviour.Properties unbreakableStone = BlockBehaviour.Properties.of(Material.STONE).strength(-1.0F);
    private static final BlockBehaviour.Properties onePunchStone = BlockBehaviour.Properties.of(Material.STONE).instabreak();
    private static final BlockBehaviour.Properties unbreakableFence = BlockBehaviour.Properties.of(Material.WOOD).strength(-1.0F);

    // Create custom blocks
    public static final RegistryObject<Block> BLOCK_A = BLOCKS.register("block_a", () -> new BlockA(onePunchStone));
    public static final RegistryObject<Block> BLOCK_B = BLOCKS.register("block_b", () -> new BlockB(onePunchStone));
    public static final RegistryObject<Block> TABS_FENCE = BLOCKS.register("tabs_fence", () -> new FenceBlock(unbreakableFence));


    @SubscribeEvent
    public static void onRegisterItems(final RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
            BLOCKS.getEntries().forEach((blockRegistryObject -> {
                Block block = blockRegistryObject.get();
                Item.Properties properties = new Item.Properties().stacksTo(64);
                Supplier<Item> blockItemFactory = () -> new BlockItem(block, properties);
                event.register(ForgeRegistries.Keys.ITEMS, blockRegistryObject.getId(), blockItemFactory);
            }));
        }
    }

}
