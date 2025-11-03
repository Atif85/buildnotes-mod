package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ImageChunkS2CPacket(UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<ImageChunkS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "image_chunk_s2c"));

    public static final PacketCodec<PacketByteBuf, ImageChunkS2CPacket> CODEC = CustomPayload.codecOf(
            ImageChunkS2CPacket::write,
            ImageChunkS2CPacket::new
    );

    public ImageChunkS2CPacket(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readString(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray());
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(buildId);
        buf.writeString(filename);
        buf.writeVarInt(totalChunks);
        buf.writeVarInt(chunkIndex);
        buf.writeByteArray(data);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

