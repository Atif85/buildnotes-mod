package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;


import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record DeleteNoteC2SPacket(UUID noteId) implements CustomPacketPayload {

    // 1. Define the unique TYPE for this packet
    public static final Type<DeleteNoteC2SPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "delete_note_c2s"));

    // 2. Define the CODEC which knows how to read/write this packet
    public static final StreamCodec<FriendlyByteBuf, DeleteNoteC2SPacket> CODEC = CustomPacketPayload.codec(
            DeleteNoteC2SPacket::write, // The write method
            DeleteNoteC2SPacket::new    // The read method (constructor that takes a buffer)
    );

    // This is the constructor used by the CODEC for reading the packet
    public DeleteNoteC2SPacket(FriendlyByteBuf buf) {
        this(buf.readUUID());
    }

    // This is the method used by the CODEC for writing the packet
    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.noteId);
    }

    // 3. You MUST override this method to return your packet's TYPE
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}