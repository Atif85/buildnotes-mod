package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record InitialSyncS2CPacket(List<Note> notes, List<Build> builds) implements CustomPacketPayload {
    public static final Type<InitialSyncS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "initial_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, InitialSyncS2CPacket> CODEC = CustomPacketPayload.codec(
            InitialSyncS2CPacket::write,
            InitialSyncS2CPacket::new
    );

    public InitialSyncS2CPacket(FriendlyByteBuf buf) {
        this(buf.readList(Note::fromBuf), buf.readList(Build::fromBuf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(notes, (b, n) -> n.writeToBuf(b));
        buf.writeCollection(builds, (b, B) -> B.writeToBuf(b));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

