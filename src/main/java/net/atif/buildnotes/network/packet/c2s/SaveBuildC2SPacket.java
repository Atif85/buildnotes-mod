package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveBuildC2SPacket(Build build) implements CustomPacketPayload {
    public static final Type<SaveBuildC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "save_build_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SaveBuildC2SPacket> CODEC = CustomPacketPayload.codec(
            SaveBuildC2SPacket::write,
            SaveBuildC2SPacket::new
    );

    public SaveBuildC2SPacket(FriendlyByteBuf buf) {
        this(Build.fromBuf(buf));
    }

    public void write(FriendlyByteBuf buf) { build.writeToBuf(buf); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

