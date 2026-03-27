package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ImageNotFoundS2CPacket(UUID buildId, String filename) implements CustomPacketPayload {
    public static final Type<ImageNotFoundS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "image_not_found_s2c"));

    public static final StreamCodec<FriendlyByteBuf, ImageNotFoundS2CPacket> CODEC = CustomPacketPayload.codec(
            ImageNotFoundS2CPacket::write,
            ImageNotFoundS2CPacket::new
    );

    public ImageNotFoundS2CPacket(FriendlyByteBuf buf) { this(buf.readUUID(), buf.readUtf()); }

    public void write(FriendlyByteBuf buf) { buf.writeUUID(buildId); buf.writeUtf(filename); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

