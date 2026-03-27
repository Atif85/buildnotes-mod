package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ImageChunkS2CPacket(UUID buildId, String filename, int totalChunks, int chunkIndex, byte[] data) implements CustomPacketPayload {
    public static final Type<ImageChunkS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "image_chunk_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ImageChunkS2CPacket> CODEC = CustomPacketPayload.codec(
            ImageChunkS2CPacket::write,
            ImageChunkS2CPacket::new
    );

    public ImageChunkS2CPacket(FriendlyByteBuf buf) {
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

