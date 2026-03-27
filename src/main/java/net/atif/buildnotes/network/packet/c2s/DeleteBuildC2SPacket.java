package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


import java.util.UUID;

public record DeleteBuildC2SPacket(UUID buildId) implements CustomPacketPayload {
    public static final Type<DeleteBuildC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "delete_build_c2s"));

    public static final StreamCodec<FriendlyByteBuf, DeleteBuildC2SPacket> CODEC = CustomPacketPayload.codec(
            DeleteBuildC2SPacket::write,
            DeleteBuildC2SPacket::new
    );

    public DeleteBuildC2SPacket(FriendlyByteBuf buf) { this(buf.readUUID()); }

    public void write(FriendlyByteBuf buf) { buf.writeUUID(this.buildId); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

