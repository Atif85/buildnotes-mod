package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RequestImageC2SPacket(UUID buildId, String filename) implements CustomPacketPayload {
    public static final Type<RequestImageC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "request_image_c2s"));

    public static final StreamCodec<FriendlyByteBuf, RequestImageC2SPacket> CODEC = CustomPacketPayload.codec(
            RequestImageC2SPacket::write,
            RequestImageC2SPacket::new
    );

    public RequestImageC2SPacket(FriendlyByteBuf buf) { this(buf.readUUID(), buf.readUtf()); }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(buildId);
        buf.writeUtf(filename);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

