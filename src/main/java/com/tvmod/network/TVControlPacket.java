package com.tvmod.network;

import com.tvmod.tileentity.TVTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class TVControlPacket {

    private final BlockPos pos;
    private final TVAction action;
    private final String url;
    private final float volume;
    private final long seekPosition;

    public TVControlPacket(BlockPos pos, TVAction action, String url, float volume, long seekPosition) {
        this.pos = pos;
        this.action = action;
        this.url = url != null ? url : "";
        this.volume = volume;
        this.seekPosition = seekPosition;
    }

    public static TVControlPacket play(BlockPos pos) {
        return new TVControlPacket(pos, TVAction.PLAY, "", 1.0f, 0);
    }

    public static TVControlPacket pause(BlockPos pos) {
        return new TVControlPacket(pos, TVAction.PAUSE, "", 1.0f, 0);
    }

    public static TVControlPacket stop(BlockPos pos) {
        return new TVControlPacket(pos, TVAction.STOP, "", 1.0f, 0);
    }

    public static TVControlPacket setUrl(BlockPos pos, String url) {
        return new TVControlPacket(pos, TVAction.SET_URL, url, 1.0f, 0);
    }

    public static TVControlPacket setVolume(BlockPos pos, float volume) {
        return new TVControlPacket(pos, TVAction.SET_VOLUME, "", volume, 0);
    }

    public static TVControlPacket seek(BlockPos pos, long position) {
        return new TVControlPacket(pos, TVAction.SEEK, "", 1.0f, position);
    }

    public void encode(PacketBuffer buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(action);
        buf.writeUtf(url, 2048);
        buf.writeFloat(volume);
        buf.writeLong(seekPosition);
    }

    public static TVControlPacket decode(PacketBuffer buf) {
        BlockPos pos = buf.readBlockPos();
        TVAction action = buf.readEnum(TVAction.class);
        String url = buf.readUtf(2048);
        float volume = buf.readFloat();
        long seekPosition = buf.readLong();
        return new TVControlPacket(pos, action, url, volume, seekPosition);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerWorld world = ctx.get().getSender().getLevel();
            TileEntity te = world.getBlockEntity(pos);

            if (te instanceof TVTileEntity) {
                TVTileEntity tv = (TVTileEntity) te;

                switch (action) {
                    case PLAY: tv.play(); break;
                    case PAUSE: tv.pause(); break;
                    case STOP: tv.stop(); break;
                    case SET_URL: tv.setVideoUrl(url); break;
                    case SET_VOLUME: tv.setVolume(volume); break;
                    case SEEK: tv.setPlaybackPosition(seekPosition); break;
                }

                tv.syncToClients();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() { return pos; }
    public TVAction getAction() { return action; }
    public String getUrl() { return url; }
    public float getVolume() { return volume; }
    public long getSeekPosition() { return seekPosition; }
}
