package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ImageNotFoundS2CPacket(UUID buildId, String filename) implements CustomPayload {
    public static final CustomPayload.Id<ImageNotFoundS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "image_not_found_s2c"));

    public static final PacketCodec<PacketByteBuf, ImageNotFoundS2CPacket> CODEC = CustomPayload.codecOf(
            ImageNotFoundS2CPacket::write,
            ImageNotFoundS2CPacket::new
    );

    public ImageNotFoundS2CPacket(PacketByteBuf buf) { this(buf.readUuid(), buf.readString()); }

    public void write(PacketByteBuf buf) { buf.writeUuid(buildId); buf.writeString(filename); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

