package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateBuildS2CPacket(Build build) implements CustomPayload {
    public static final CustomPayload.Id<UpdateBuildS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "update_build_s2c"));

    public static final PacketCodec<PacketByteBuf, UpdateBuildS2CPacket> CODEC = CustomPayload.codecOf(
            UpdateBuildS2CPacket::write,
            UpdateBuildS2CPacket::new
    );

    public UpdateBuildS2CPacket(PacketByteBuf buf) { this(Build.fromBuf(buf)); }

    public void write(PacketByteBuf buf) { build.writeToBuf(buf); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

