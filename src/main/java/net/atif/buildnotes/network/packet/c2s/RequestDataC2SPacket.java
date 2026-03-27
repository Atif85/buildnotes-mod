package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestDataC2SPacket() implements CustomPacketPayload {
    public static final Type<RequestDataC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "request_data_c2s"));

    public static final StreamCodec<FriendlyByteBuf, RequestDataC2SPacket> CODEC = CustomPacketPayload.codec(
            RequestDataC2SPacket::write,
            RequestDataC2SPacket::new
    );

    public RequestDataC2SPacket(FriendlyByteBuf buf) { this(); }

    public void write(FriendlyByteBuf buf) { /* no payload */ }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

