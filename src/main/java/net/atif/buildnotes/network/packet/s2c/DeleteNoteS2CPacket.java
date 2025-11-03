package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DeleteNoteS2CPacket(UUID noteId) implements CustomPayload {
    public static final CustomPayload.Id<DeleteNoteS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "delete_note_s2c"));

    public static final PacketCodec<PacketByteBuf, DeleteNoteS2CPacket> CODEC = CustomPayload.codecOf(
            DeleteNoteS2CPacket::write,
            DeleteNoteS2CPacket::new
    );

    public DeleteNoteS2CPacket(PacketByteBuf buf) { this(buf.readUuid()); }

    public void write(PacketByteBuf buf) { buf.writeUuid(noteId); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

