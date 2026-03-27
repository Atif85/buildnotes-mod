package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record UploadImageChunkC2SPacket(UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) implements CustomPacketPayload {
    public static final Type<UploadImageChunkC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "upload_image_chunk_c2s"));

    public static final StreamCodec<FriendlyByteBuf, UploadImageChunkC2SPacket> CODEC = CustomPacketPayload.codec(
            UploadImageChunkC2SPacket::write,
            UploadImageChunkC2SPacket::new
    );

    public UploadImageChunkC2SPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(buildId);
        buf.writeUtf(filename);
        buf.writeVarInt(totalChunks);
        buf.writeVarInt(chunkIndex);
        buf.writeByteArray(data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

