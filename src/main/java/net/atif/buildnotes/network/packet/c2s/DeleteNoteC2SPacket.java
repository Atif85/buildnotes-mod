package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;


import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DeleteNoteC2SPacket(UUID noteId) implements CustomPayload {

    // 1. Define the unique ID for this packet
    public static final CustomPayload.Id<DeleteNoteC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "delete_note_c2s"));

    // 2. Define the CODEC which knows how to read/write this packet
    public static final PacketCodec<PacketByteBuf, DeleteNoteC2SPacket> CODEC = CustomPayload.codecOf(
            DeleteNoteC2SPacket::write, // The write method
            DeleteNoteC2SPacket::new    // The read method (constructor that takes a buffer)
    );

    // This is the constructor used by the CODEC for reading the packet
    public DeleteNoteC2SPacket(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    // This is the method used by the CODEC for writing the packet
    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.noteId);
    }

    // 3. You MUST override this method to return your packet's ID
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}