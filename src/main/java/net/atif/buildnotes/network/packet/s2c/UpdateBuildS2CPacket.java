package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateBuildS2CPacket(Build build) implements CustomPacketPayload {
    public static final Type<UpdateBuildS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "update_build_s2c"));

    public static final StreamCodec<FriendlyByteBuf, UpdateBuildS2CPacket> CODEC = CustomPacketPayload.codec(
            UpdateBuildS2CPacket::write,
            UpdateBuildS2CPacket::new
    );

    public UpdateBuildS2CPacket(FriendlyByteBuf buf) { this(Build.fromBuf(buf)); }

    public void write(FriendlyByteBuf buf) { build.writeToBuf(buf); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

