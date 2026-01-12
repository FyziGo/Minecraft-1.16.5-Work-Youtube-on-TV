package com.tvmod.init;

import com.tvmod.TVMod;
import com.tvmod.tileentity.TVTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModTileEntities {
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, TVMod.MOD_ID);

    public static final RegistryObject<TileEntityType<TVTileEntity>> TV_TILE_ENTITY = 
            TILE_ENTITIES.register("tv_tile_entity", 
                    () -> TileEntityType.Builder.of(TVTileEntity::new, ModBlocks.TV_BLOCK.get()).build(null));

    public static void register(IEventBus eventBus) {
        TILE_ENTITIES.register(eventBus);
    }
}
