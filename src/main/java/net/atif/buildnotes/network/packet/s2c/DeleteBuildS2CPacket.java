package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DeleteBuildS2CPacket(UUID buildId) implements CustomPayload {
    public static final CustomPayload.Id<DeleteBuildS2CPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "delete_build_s2c"));

    public static final PacketCodec<PacketByteBuf, DeleteBuildS2CPacket> CODEC = CustomPayload.codecOf(
            DeleteBuildS2CPacket::write,
            DeleteBuildS2CPacket::new
    );

    public DeleteBuildS2CPacket(PacketByteBuf buf) { this(buf.readUuid()); }

    public void write(PacketByteBuf buf) { buf.writeUuid(buildId); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

