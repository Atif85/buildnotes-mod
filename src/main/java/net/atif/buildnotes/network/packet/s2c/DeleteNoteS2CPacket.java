package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record DeleteNoteS2CPacket(UUID noteId) implements CustomPacketPayload {
    public static final Type<DeleteNoteS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "delete_note_s2c"));

    public static final StreamCodec<FriendlyByteBuf, DeleteNoteS2CPacket> CODEC = CustomPacketPayload.codec(
            DeleteNoteS2CPacket::write,
            DeleteNoteS2CPacket::new
    );

    public DeleteNoteS2CPacket(FriendlyByteBuf buf) { this(buf.readUUID()); }

    public void write(FriendlyByteBuf buf) { buf.writeUUID(noteId); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

