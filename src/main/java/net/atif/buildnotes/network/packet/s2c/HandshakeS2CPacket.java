package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.PermissionLevel;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HandshakeS2CPacket(PermissionLevel permission) implements CustomPayload {
    public static final CustomPayload.Id<HandshakeS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "handshake_s2c"));

    public static final PacketCodec<PacketByteBuf, HandshakeS2CPacket> CODEC = CustomPayload.codecOf(
            HandshakeS2CPacket::write,
            HandshakeS2CPacket::new
    );

    public HandshakeS2CPacket(PacketByteBuf buf) { this(buf.readEnumConstant(PermissionLevel.class)); }

    public void write(PacketByteBuf buf) { buf.writeEnumConstant(permission); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

