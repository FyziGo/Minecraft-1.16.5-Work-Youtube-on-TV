package com.tvmod.tileentity;

import com.tvmod.init.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

import javax.annotation.Nullable;

public class TVTileEntity extends TileEntity {

    private String videoUrl = "";
    private boolean isPlaying = false;
    private long playbackPosition = 0;
    private float volume = 1.0f;
    private int screenSize = 2;
    private int qualityIndex = 1;
    private int sourceIndex = 1;
    private int speedIndex = 3;

    public static final int[] VALID_SIZES = {1, 2, 4, 6, 8, 10, 12};
    public static final String[] QUALITY_OPTIONS = {"low", "medium", "high", "dash"};
    public static final float[] SPEED_OPTIONS = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};

    public TVTileEntity() {
        super(ModTileEntities.TV_TILE_ENTITY.get());
    }

    protected TVTileEntity(TileEntityType<?> type) {
        super(type);
    }

    public void play() {
        if (!videoUrl.isEmpty()) {
            this.isPlaying = true;
            setChanged();
        }
    }

    public void pause() {
        if (this.isPlaying) {
            this.isPlaying = false;
            setChanged();
        }
    }

    public void stop() {
        this.isPlaying = false;
        this.playbackPosition = 0;
        setChanged();
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        setChanged();
    }

    public void setVideoUrl(String url) {
        this.videoUrl = url != null ? url : "";
        setChanged();
    }

    public void setPlaybackPosition(long position) {
        this.playbackPosition = Math.max(0, position);
        setChanged();
    }

    public String getVideoUrl() { return videoUrl; }
    public boolean isPlaying() { return isPlaying; }
    public long getPlaybackPosition() { return playbackPosition; }
    public float getVolume() { return volume; }
    public int getScreenSize() { return screenSize; }

    public void setScreenSize(int size) {
        for (int validSize : VALID_SIZES) {
            if (size == validSize) {
                this.screenSize = size;
                setChanged();
                return;
            }
        }
        this.screenSize = 2;
        setChanged();
    }

    public int getQualityIndex() { return qualityIndex; }
    public void setQualityIndex(int index) {
        if (index >= 0 && index < QUALITY_OPTIONS.length) {
            this.qualityIndex = index;
            setChanged();
        }
    }

    public String getQuality() {
        if (qualityIndex >= 0 && qualityIndex < QUALITY_OPTIONS.length) {
            return QUALITY_OPTIONS[qualityIndex];
        }
        return "medium";
    }

    public int getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(int index) {
        if (index >= 0 && index <= 5) {
            this.sourceIndex = index;
            setChanged();
        }
    }

    public int getSpeedIndex() { return speedIndex; }
    public void setSpeedIndex(int index) {
        if (index >= 0 && index < SPEED_OPTIONS.length) {
            this.speedIndex = index;
            setChanged();
        }
    }

    public float getSpeed() {
        if (speedIndex >= 0 && speedIndex < SPEED_OPTIONS.length) {
            return SPEED_OPTIONS[speedIndex];
        }
        return 1.0f;
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putString("VideoUrl", videoUrl);
        tag.putBoolean("Playing", isPlaying);
        tag.putLong("Position", playbackPosition);
        tag.putFloat("Volume", volume);
        tag.putInt("ScreenSize", screenSize);
        tag.putInt("QualityIndex", qualityIndex);
        tag.putInt("SourceIndex", sourceIndex);
        tag.putInt("SpeedIndex", speedIndex);
        return tag;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        this.videoUrl = tag.getString("VideoUrl");
        this.isPlaying = false;
        this.playbackPosition = tag.getLong("Position");
        this.volume = tag.getFloat("Volume");
        if (Float.isNaN(this.volume) || this.volume < 0.0f || this.volume > 1.0f) {
            this.volume = 1.0f;
        }
        int loadedSize = tag.getInt("ScreenSize");
        this.screenSize = 2;
        for (int validSize : VALID_SIZES) {
            if (loadedSize == validSize) {
                this.screenSize = loadedSize;
                break;
            }
        }
        int loadedQuality = tag.getInt("QualityIndex");
        this.qualityIndex = (loadedQuality >= 0 && loadedQuality < QUALITY_OPTIONS.length) ? loadedQuality : 1;
        int loadedSource = tag.getInt("SourceIndex");
        this.sourceIndex = (loadedSource >= 0 && loadedSource <= 5) ? loadedSource : 1;
        int loadedSpeed = tag.getInt("SpeedIndex");
        this.speedIndex = (loadedSpeed >= 0 && loadedSpeed < SPEED_OPTIONS.length) ? loadedSpeed : 3;
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT tag = super.getUpdateTag();
        tag.putString("VideoUrl", videoUrl);
        tag.putBoolean("Playing", isPlaying);
        tag.putLong("Position", playbackPosition);
        tag.putFloat("Volume", volume);
        tag.putInt("ScreenSize", screenSize);
        tag.putInt("QualityIndex", qualityIndex);
        tag.putInt("SourceIndex", sourceIndex);
        tag.putInt("SpeedIndex", speedIndex);
        return tag;
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.videoUrl = tag.getString("VideoUrl");
        this.isPlaying = tag.getBoolean("Playing");
        this.playbackPosition = tag.getLong("Position");
        this.volume = tag.getFloat("Volume");
        if (Float.isNaN(this.volume) || this.volume < 0.0f || this.volume > 1.0f) {
            this.volume = 1.0f;
        }
        int loadedSize = tag.getInt("ScreenSize");
        this.screenSize = 2;
        for (int validSize : VALID_SIZES) {
            if (loadedSize == validSize) {
                this.screenSize = loadedSize;
                break;
            }
        }
        int loadedQuality = tag.getInt("QualityIndex");
        this.qualityIndex = (loadedQuality >= 0 && loadedQuality < QUALITY_OPTIONS.length) ? loadedQuality : 1;
        int loadedSource = tag.getInt("SourceIndex");
        this.sourceIndex = (loadedSource >= 0 && loadedSource <= 5) ? loadedSource : 1;
        int loadedSpeed = tag.getInt("SpeedIndex");
        this.speedIndex = (loadedSpeed >= 0 && loadedSpeed < SPEED_OPTIONS.length) ? loadedSpeed : 3;
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.worldPosition, 1, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        handleUpdateTag(getBlockState(), pkt.getTag());
    }

    public void syncToClients() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
