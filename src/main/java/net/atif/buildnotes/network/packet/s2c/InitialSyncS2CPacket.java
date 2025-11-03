package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record InitialSyncS2CPacket(List<Note> notes, List<Build> builds) implements CustomPayload {
    public static final CustomPayload.Id<InitialSyncS2CPacket> ID = new CustomPayload.Id<>(new Identifier(Buildnotes.MOD_ID, "initial_sync_s2c"));

    public static final PacketCodec<PacketByteBuf, InitialSyncS2CPacket> CODEC = CustomPayload.codecOf(
            InitialSyncS2CPacket::write,
            InitialSyncS2CPacket::new
    );

    public InitialSyncS2CPacket(PacketByteBuf buf) {
        this(buf.readList(Note::fromBuf), buf.readList(Build::fromBuf));
    }

    public void write(PacketByteBuf buf) {
        buf.writeCollection(notes, (b, n) -> n.writeToBuf(b));
        buf.writeCollection(builds, (b, B) -> B.writeToBuf(b));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

