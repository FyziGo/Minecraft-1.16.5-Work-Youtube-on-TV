package com.tvmod.client;

import com.tvmod.TVMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Video player for TV blocks. Handles video playback and texture rendering.
 * Uses WATERMeDIA for actual video playback when available.
 * 
 * Requirements: 3.1, 3.4, 4.1, 4.2, 4.3, 4.4
 */
@OnlyIn(Dist.CLIENT)
public class TVVideoPlayer {

    private final BlockPos pos;
    private DynamicTexture texture;
    private ResourceLocation textureLocation;
    private NativeImage frameBuffer;

    private int textureWidth = 854;
    private int textureHeight = 480;

    private String currentUrl = "";
    private boolean isPlaying = false;
    private long playbackPosition = 0;
    private long startTime = 0;
    private float volume = 1.0f;

    // WATERMeDIA player instance
    private Object waterMediaPlayer = null;
    private boolean waterMediaAvailable = false;

    // MCEF player (alternative)
    private MCEFVideoPlayer mcefPlayer = null;
    private boolean useMCEF = false;

    // Optimization: only update texture every N ticks
    private int tickCounter = 0;
    private static final int TEXTURE_UPDATE_INTERVAL = 10;
    private boolean textureNeedsUpdate = true;

    public TVVideoPlayer(BlockPos pos) {
        this.pos = pos;
        initializeTexture();
        checkWaterMediaAvailability();
    }

    private void checkWaterMediaAvailability() {
        try {
            Class.forName("me.srrapero720.watermedia.api.player.SyncVideoPlayer");
            waterMediaAvailable = true;
            TVMod.LOGGER.info("WATERMeDIA detected at {}", pos);
            return;
        } catch (ClassNotFoundException e) {
            waterMediaAvailable = false;
        }

        try {
            Class.forName("net.montoyo.mcef.api.API");
            useMCEF = true;
            mcefPlayer = new MCEFVideoPlayer(pos);
            TVMod.LOGGER.info("MCEF detected, using browser playback at {}", pos);
            return;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.cinemamod.mcef.MCEF");
                useMCEF = true;
                mcefPlayer = new MCEFVideoPlayer(pos);
                TVMod.LOGGER.info("CinemaMod MCEF detected, using browser playback at {}", pos);
                return;
            } catch (ClassNotFoundException e2) {
            }
        }

        TVMod.LOGGER.warn("No video backend found (WATERMeDIA or MCEF) - using simulated playback at {}", pos);
    }

    private void initializeTexture() {
        try {
            this.frameBuffer = new NativeImage(textureWidth, textureHeight, false);
            fillWithColor(0xFF000000);
            this.texture = new DynamicTexture(frameBuffer);
            this.textureLocation = Minecraft.getInstance().getTextureManager()
                    .register("tv_screen_" + pos.hashCode(), texture);
            TVMod.LOGGER.debug("Initialized TV texture at {}", pos);
        } catch (Exception e) {
            TVMod.LOGGER.error("Failed to initialize TV texture at {}: {}", pos, e.getMessage());
        }
    }

    private void fillWithColor(int color) {
        if (frameBuffer != null) {
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    frameBuffer.setPixelRGBA(x, y, color);
                }
            }
        }
    }

    private void fillWithNoise() {
        if (frameBuffer != null) {
            long time = System.currentTimeMillis();
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int noise = (int) ((Math.sin(x * 0.1 + time * 0.01) + 
                                       Math.cos(y * 0.1 + time * 0.008)) * 64 + 128);
                    noise = Math.max(0, Math.min(255, noise));
                    int color = 0xFF000000 | (noise << 16) | (noise << 8) | noise;
                    frameBuffer.setPixelRGBA(x, y, color);
                }
            }
        }
    }

    private void fillWithPlayingIndicator() {
        if (frameBuffer != null) {
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int blue = 100 + (y * 100 / textureHeight);
                    int green = 50 + (x * 50 / textureWidth);
                    int color = 0xFF000000 | (blue << 16) | (green << 8) | 30;
                    frameBuffer.setPixelRGBA(x, y, color);
                }
            }
        }
    }

    public void play(String url) {
        play(url, "medium", 1);
    }

    public void play(String url, String quality) {
        play(url, quality, 1);
    }

    public void play(String url, String quality, int sourceIndex) {
        if (url == null || url.isEmpty()) {
            TVMod.LOGGER.warn("Cannot play empty URL at {}", pos);
            return;
        }

        this.currentUrl = url;
        this.isPlaying = true;
        this.playbackPosition = 0;
        this.startTime = System.currentTimeMillis();
        textureNeedsUpdate = true;

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.setQuality(quality);
            mcefPlayer.setSourceIndex(sourceIndex);
            mcefPlayer.play(url, quality, sourceIndex);
            TVMod.LOGGER.info("MCEF playback started for {} at {} with quality {} source {}", 
                    url, pos, quality, MCEFVideoPlayer.VIDEO_SOURCES[sourceIndex]);
        } else if (waterMediaAvailable) {
            startWaterMediaPlayback(url);
        } else {
            TVMod.LOGGER.info("Simulated playback started for {} at {}", url, pos);
        }
    }

    private void startWaterMediaPlayback(String url) {
        try {
            Class<?> syncPlayerClass = Class.forName("me.srrapero720.watermedia.api.player.SyncVideoPlayer");
            if (waterMediaPlayer == null) {
                waterMediaPlayer = syncPlayerClass.getConstructor().newInstance();
            }
            syncPlayerClass.getMethod("start", String.class).invoke(waterMediaPlayer, url);
            TVMod.LOGGER.info("WATERMeDIA playback started for {} at {}", url, pos);
        } catch (Exception e) {
            TVMod.LOGGER.error("Failed to start WATERMeDIA playback: {}", e.getMessage());
            waterMediaAvailable = false;
            fillWithPlayingIndicator();
            if (texture != null) {
                texture.upload();
            }
        }
    }

    public void pause() {
        if (!isPlaying) return;
        this.isPlaying = false;
        this.playbackPosition = System.currentTimeMillis() - startTime;

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.pause();
        } else if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("pause").invoke(waterMediaPlayer);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to pause WATERMeDIA: {}", e.getMessage());
            }
        }

        fillWithColor(0xFF222244);
        if (texture != null) {
            texture.upload();
        }
        TVMod.LOGGER.debug("Paused video at {}", pos);
    }

    public void resume() {
        if (isPlaying || currentUrl.isEmpty()) return;
        this.isPlaying = true;
        this.startTime = System.currentTimeMillis() - playbackPosition;

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.resume();
        } else if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("play").invoke(waterMediaPlayer);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to resume WATERMeDIA: {}", e.getMessage());
            }
        }
        TVMod.LOGGER.debug("Resumed video at {}", pos);
    }

    public void stop() {
        this.isPlaying = false;
        this.playbackPosition = 0;
        this.startTime = 0;

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.stop();
        } else if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("stop").invoke(waterMediaPlayer);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to stop WATERMeDIA: {}", e.getMessage());
            }
        }

        fillWithColor(0xFF000000);
        if (texture != null) {
            texture.upload();
        }
        TVMod.LOGGER.debug("Stopped video at {}", pos);
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.setVolume(this.volume);
        } else if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("setVolume", int.class)
                        .invoke(waterMediaPlayer, (int) (this.volume * 100));
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to set volume: {}", e.getMessage());
            }
        }
    }

    public void setPlaybackSpeed(float speed) {
        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.setPlaybackSpeed(speed);
        } else if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("setRate", float.class)
                        .invoke(waterMediaPlayer, speed);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to set playback speed: {}", e.getMessage());
            }
        }
        TVMod.LOGGER.debug("Playback speed set to {}x at {}", speed, pos);
    }

    public void seekTo(long position) {
        this.playbackPosition = position;
        this.startTime = System.currentTimeMillis() - position;

        if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("seekTo", long.class).invoke(waterMediaPlayer, position);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Failed to seek: {}", e.getMessage());
            }
        }
    }

    public boolean isPlaying() { return isPlaying; }

    public long getPosition() {
        if (isPlaying) {
            return System.currentTimeMillis() - startTime;
        }
        return playbackPosition;
    }

    public long getDuration() {
        if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                return (Long) waterMediaPlayer.getClass().getMethod("getDuration").invoke(waterMediaPlayer);
            } catch (Exception e) {}
        }
        return 0;
    }

    @Nullable
    public ResourceLocation getTextureLocation() { return textureLocation; }
    public String getCurrentUrl() { return currentUrl; }
    public float getVolume() { return volume; }
    public BlockPos getPos() { return pos; }
    public int getTextureWidth() { return textureWidth; }
    public int getTextureHeight() { return textureHeight; }

    public void tick(PlayerEntity player) {
        if (!isPlaying) return;
        tickCounter++;

        if (useMCEF && mcefPlayer != null) {
            mcefPlayer.tick(player);
        }

        if (tickCounter % TEXTURE_UPDATE_INTERVAL == 0 || textureNeedsUpdate) {
            if (!waterMediaAvailable || waterMediaPlayer == null) {
                fillWithPlayingIndicator();
                if (texture != null) {
                    texture.upload();
                }
            }
            textureNeedsUpdate = false;
        }

        if (tickCounter % 20 == 0 && player != null) {
            updateSpatialAudio(player);
        }
    }

    private void updateWaterMediaTexture() {
        try {
            Object textureId = waterMediaPlayer.getClass().getMethod("texture").invoke(waterMediaPlayer);
            if (textureId instanceof Integer && (Integer) textureId > 0) {
            }
        } catch (Exception e) {}
    }

    private void updateSpatialAudio(PlayerEntity player) {
        double distance = Math.sqrt(player.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        float spatialVolume = VolumeCalculator.calculateVolume(distance, volume);

        if (waterMediaAvailable && waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("setVolume", int.class)
                        .invoke(waterMediaPlayer, (int) (spatialVolume * 100));
            } catch (Exception e) {}
        }
    }

    public void release() {
        TVMod.LOGGER.debug("Releasing TV video player at {}", pos);

        if (mcefPlayer != null) {
            mcefPlayer.release();
            mcefPlayer = null;
        }

        if (waterMediaPlayer != null) {
            try {
                waterMediaPlayer.getClass().getMethod("stop").invoke(waterMediaPlayer);
                waterMediaPlayer.getClass().getMethod("release").invoke(waterMediaPlayer);
            } catch (Exception e) {
                TVMod.LOGGER.debug("Error releasing WATERMeDIA: {}", e.getMessage());
            }
            waterMediaPlayer = null;
        }

        if (texture != null) {
            try { texture.close(); } catch (Exception e) {}
            texture = null;
        }

        if (frameBuffer != null) {
            try { frameBuffer.close(); } catch (Exception e) {}
            frameBuffer = null;
        }

        if (textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
            textureLocation = null;
        }

        isPlaying = false;
        currentUrl = "";
        playbackPosition = 0;
    }

    public boolean isWaterMediaAvailable() { return waterMediaAvailable; }
    public boolean isMCEFAvailable() { return useMCEF && mcefPlayer != null; }

    @Nullable
    public MCEFVideoPlayer getMCEFPlayer() {
        if (useMCEF && mcefPlayer != null) return mcefPlayer;
        return null;
    }

    public int getMCEFTextureId() {
        if (useMCEF && mcefPlayer != null) return mcefPlayer.getTextureId();
        return -1;
    }

    @Nullable
    public ResourceLocation getMCEFTextureLocation() {
        if (useMCEF && mcefPlayer != null) {
            ResourceLocation texLoc = mcefPlayer.getTextureLocationAsResource();
            if (texLoc != null) return texLoc;
            Object texLocObj = mcefPlayer.getTextureLocation();
            if (texLocObj instanceof ResourceLocation) return (ResourceLocation) texLocObj;
        }
        return null;
    }
}
