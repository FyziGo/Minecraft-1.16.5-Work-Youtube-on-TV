package com.tvmod;

import com.tvmod.client.renderer.TVBlockRenderer;
import com.tvmod.init.ModBlocks;
import com.tvmod.init.ModItems;
import com.tvmod.init.ModTileEntities;
import com.tvmod.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TVMod.MOD_ID)
public class TVMod {
    public static final String MOD_ID = "tvmod";
    public static final Logger LOGGER = LogManager.getLogger();

    public TVMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModTileEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("TV Mod initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
        LOGGER.info("TV Mod common setup - network registered");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.TV_TILE_ENTITY.get(),
                TVBlockRenderer::new
        );
        LOGGER.info("TV Mod client setup - renderer registered");
    }
}
