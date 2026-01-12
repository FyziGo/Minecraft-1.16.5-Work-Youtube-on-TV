package com.tvmod.client;

import com.tvmod.TVMod;
import com.tvmod.block.TVBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TVMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static int tickCounter = 0;
    private static final int CLEANUP_INTERVAL = 5;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        VideoPlayerManager.tick();

        tickCounter++;
        if (tickCounter >= CLEANUP_INTERVAL) {
            tickCounter = 0;
            VideoPlayerManager.cleanupInvalidBlocks();
            VideoPlayerManager.cleanupDistant(64.0);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            TVMod.LOGGER.info("World unloading - clearing video players");
            VideoPlayerManager.clear();
        }
    }
}
