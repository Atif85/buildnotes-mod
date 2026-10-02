package net.atif.buildnotes.network.packet.s2c;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.Note;
import net.minecraft.network.FriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public record InitialSyncS2CPacket(List<Note> notes, List<Build> builds) implements CustomPacketPayload {
    public static final Type<InitialSyncS2CPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Buildnotes.MOD_ID, "initial_sync_s2c"));

    public static final StreamCodec<FriendlyByteBuf, InitialSyncS2CPacket> CODEC = CustomPacketPayload.codec(
            InitialSyncS2CPacket::write,
            InitialSyncS2CPacket::new
    );

    public InitialSyncS2CPacket(@NonNull FriendlyByteBuf buf) {
        this(readNotes(buf), readBuilds(buf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(notes.size());
        for (Note note : notes) {
            note.writeToBuf(buf);
        }

        buf.writeVarInt(builds.size());
        for (Build build : builds) {
            build.writeToBuf(buf);
        }
    }

    private static List<Note> readNotes(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Note> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Note.fromBuf(buf));
        }
        return list;
    }

    private static List<Build> readBuilds(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Build> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(Build.fromBuf(buf));
        }
        return list;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

