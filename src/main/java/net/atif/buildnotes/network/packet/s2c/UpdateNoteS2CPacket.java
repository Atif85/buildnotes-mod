package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateNoteS2CPacket(Note note) implements CustomPayload {
    public static final CustomPayload.Id<UpdateNoteS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "update_note_s2c"));

    public static final PacketCodec<PacketByteBuf, UpdateNoteS2CPacket> CODEC = CustomPayload.codecOf(
            UpdateNoteS2CPacket::write,
            UpdateNoteS2CPacket::new
    );

    public UpdateNoteS2CPacket(PacketByteBuf buf) { this(Note.fromBuf(buf)); }

    public void write(PacketByteBuf buf) { note.writeToBuf(buf); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

