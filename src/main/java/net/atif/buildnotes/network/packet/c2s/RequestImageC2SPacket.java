package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestImageC2SPacket(UUID buildId, String filename) implements CustomPayload {
    public static final CustomPayload.Id<RequestImageC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "request_image_c2s"));

    public static final PacketCodec<PacketByteBuf, RequestImageC2SPacket> CODEC = CustomPayload.codecOf(
            RequestImageC2SPacket::write,
            RequestImageC2SPacket::new
    );

    public RequestImageC2SPacket(PacketByteBuf buf) { this(buf.readUuid(), buf.readString()); }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(buildId);
        buf.writeString(filename);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

