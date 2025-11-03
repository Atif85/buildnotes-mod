package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DeleteBuildC2SPacket(UUID buildId) implements CustomPayload {
    public static final CustomPayload.Id<DeleteBuildC2SPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "delete_build_c2s"));

    public static final PacketCodec<PacketByteBuf, DeleteBuildC2SPacket> CODEC = CustomPayload.codecOf(
            DeleteBuildC2SPacket::write,
            DeleteBuildC2SPacket::new
    );

    public DeleteBuildC2SPacket(PacketByteBuf buf) { this(buf.readUuid()); }

    public void write(PacketByteBuf buf) { buf.writeUuid(this.buildId); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

