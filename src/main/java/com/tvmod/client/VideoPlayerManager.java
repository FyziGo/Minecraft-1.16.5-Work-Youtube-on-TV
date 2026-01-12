package com.tvmod.client;

import com.tvmod.TVMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class VideoPlayerManager {

    private static final Map<BlockPos, TVVideoPlayer> players = new HashMap<>();

    public static TVVideoPlayer getOrCreate(BlockPos pos) {
        return players.computeIfAbsent(pos, TVVideoPlayer::new);
    }

    @Nullable
    public static TVVideoPlayer get(BlockPos pos) {
        return players.get(pos);
    }

    public static boolean exists(BlockPos pos) {
        return players.containsKey(pos);
    }

    public static void remove(BlockPos pos) {
        TVVideoPlayer player = players.remove(pos);
        if (player != null) {
            player.release();
            TVMod.LOGGER.debug("Removed video player at {}", pos);
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PlayerEntity player = mc.player;

        for (TVVideoPlayer videoPlayer : players.values()) {
            try {
                videoPlayer.tick(player);
            } catch (Exception e) {
                TVMod.LOGGER.error("Error ticking video player at {}: {}", 
                        videoPlayer.getPos(), e.getMessage());
            }
        }
    }

    public static void clear() {
        TVMod.LOGGER.info("Clearing all video players ({} total)", players.size());

        for (TVVideoPlayer player : players.values()) {
            try {
                player.release();
            } catch (Exception e) {
                TVMod.LOGGER.error("Error releasing video player at {}: {}", 
                        player.getPos(), e.getMessage());
            }
        }

        players.clear();
    }

    public static void cleanupDistant(double maxDistance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        double maxDistSq = maxDistance * maxDistance;
        Iterator<Map.Entry<BlockPos, TVVideoPlayer>> iterator = players.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TVVideoPlayer> entry = iterator.next();
            BlockPos pos = entry.getKey();

            double distSq = mc.player.distanceToSqr(
                    pos.getX() + 0.5, 
                    pos.getY() + 0.5, 
                    pos.getZ() + 0.5);

            if (distSq > maxDistSq) {
                entry.getValue().release();
                iterator.remove();
                TVMod.LOGGER.debug("Cleaned up distant video player at {}", pos);
            }
        }
    }

    public static void cleanupInvalidBlocks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        if (players.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<BlockPos, TVVideoPlayer>> iterator = players.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TVVideoPlayer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            TVVideoPlayer player = entry.getValue();

            net.minecraft.block.BlockState state = mc.level.getBlockState(pos);
            boolean isTVBlock = state.getBlock() instanceof com.tvmod.block.TVBlock;

            if (!isTVBlock) {
                TVMod.LOGGER.info("TV block no longer exists at {} - stopping video player", pos);
                player.release();
                iterator.remove();
            }
        }
    }

    public static int getPlayerCount() {
        return players.size();
    }

    public static int getPlayingCount() {
        int count = 0;
        for (TVVideoPlayer player : players.values()) {
            if (player.isPlaying()) {
                count++;
            }
        }
        return count;
    }
}
