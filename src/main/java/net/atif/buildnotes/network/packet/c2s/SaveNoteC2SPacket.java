package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SaveNoteC2SPacket(Note note) implements CustomPayload {
    public static final CustomPayload.Id<SaveNoteC2SPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "save_note_c2s"));

    public static final PacketCodec<PacketByteBuf, SaveNoteC2SPacket> CODEC = CustomPayload.codecOf(
            SaveNoteC2SPacket::write,
            SaveNoteC2SPacket::new
    );

    public SaveNoteC2SPacket(PacketByteBuf buf) {
        this(Note.fromBuf(buf));
    }

    public void write(PacketByteBuf buf) {
        note.writeToBuf(buf);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

