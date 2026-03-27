package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveNoteC2SPacket(Note note) implements CustomPacketPayload {
    public static final Type<SaveNoteC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "save_note_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SaveNoteC2SPacket> CODEC = CustomPacketPayload.codec(
            SaveNoteC2SPacket::write,
            SaveNoteC2SPacket::new
    );

    public SaveNoteC2SPacket(FriendlyByteBuf buf) {
        this(Note.fromBuf(buf));
    }

    public void write(FriendlyByteBuf buf) {
        note.writeToBuf(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

