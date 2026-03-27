package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UpdateNoteS2CPacket(Note note) implements CustomPacketPayload {
    public static final Type<UpdateNoteS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "update_note_s2c"));

    public static final StreamCodec<FriendlyByteBuf, UpdateNoteS2CPacket> CODEC = CustomPacketPayload.codec(
            UpdateNoteS2CPacket::write,
            UpdateNoteS2CPacket::new
    );

    public UpdateNoteS2CPacket(FriendlyByteBuf buf) { this(Note.fromBuf(buf)); }

    public void write(FriendlyByteBuf buf) { note.writeToBuf(buf); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

