package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record UploadImageChunkC2SPacket(UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<UploadImageChunkC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "upload_image_chunk_c2s"));

    public static final PacketCodec<PacketByteBuf, UploadImageChunkC2SPacket> CODEC = CustomPayload.codecOf(
            UploadImageChunkC2SPacket::write,
            UploadImageChunkC2SPacket::new
    );

    public UploadImageChunkC2SPacket(PacketByteBuf buf) {
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

