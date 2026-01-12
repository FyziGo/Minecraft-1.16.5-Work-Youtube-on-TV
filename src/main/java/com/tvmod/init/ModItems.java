package com.tvmod.init;

import com.tvmod.TVMod;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, TVMod.MOD_ID);

    public static final RegistryObject<Item> TV_BLOCK_ITEM = ITEMS.register("tv_block",
            () -> new BlockItem(ModBlocks.TV_BLOCK.get(), 
                    new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
