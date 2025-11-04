package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestDataC2SPacket() implements CustomPayload {
    public static final CustomPayload.Id<RequestDataC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "request_data_c2s"));

    public static final PacketCodec<PacketByteBuf, RequestDataC2SPacket> CODEC = CustomPayload.codecOf(
            RequestDataC2SPacket::write,
            RequestDataC2SPacket::new
    );

    public RequestDataC2SPacket(PacketByteBuf buf) { this(); }

    public void write(PacketByteBuf buf) { /* no payload */ }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

