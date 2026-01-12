package com.tvmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import com.tvmod.tileentity.TVTileEntity;

import java.util.function.Supplier;

public class TVSyncPacket {

    private final BlockPos pos;
    private final String url;
    private final boolean playing;
    private final long position;
    private final float volume;

    public TVSyncPacket(BlockPos pos, String url, boolean playing, long position, float volume) {
        this.pos = pos;
        this.url = url != null ? url : "";
        this.playing = playing;
        this.position = position;
        this.volume = volume;
    }

    public static TVSyncPacket fromTileEntity(TVTileEntity tv) {
        return new TVSyncPacket(
            tv.getBlockPos(),
            tv.getVideoUrl(),
            tv.isPlaying(),
            tv.getPlaybackPosition(),
            tv.getVolume()
        );
    }

    public void encode(PacketBuffer buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url, 2048);
        buf.writeBoolean(playing);
        buf.writeLong(position);
        buf.writeFloat(volume);
    }

    public static TVSyncPacket decode(PacketBuffer buf) {
        BlockPos pos = buf.readBlockPos();
        String url = buf.readUtf(2048);
        boolean playing = buf.readBoolean();
        long position = buf.readLong();
        float volume = buf.readFloat();
        return new TVSyncPacket(pos, url, playing, position, volume);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            World world = Minecraft.getInstance().level;
            if (world != null) {
                TileEntity te = world.getBlockEntity(pos);
                if (te instanceof TVTileEntity) {
                    TVTileEntity tv = (TVTileEntity) te;
                    tv.setVideoUrl(url);
                    tv.setPlaybackPosition(position);
                    tv.setVolume(volume);

                    if (playing && !tv.isPlaying()) {
                        tv.play();
                    } else if (!playing && tv.isPlaying()) {
                        tv.pause();
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getPos() { return pos; }
    public String getUrl() { return url; }
    public boolean isPlaying() { return playing; }
    public long getPosition() { return position; }
    public float getVolume() { return volume; }
}
