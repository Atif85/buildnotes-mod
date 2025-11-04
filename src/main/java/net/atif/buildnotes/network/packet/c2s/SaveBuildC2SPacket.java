package net.atif.buildnotes.network.packet.c2s;

import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.data.Build;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SaveBuildC2SPacket(Build build) implements CustomPayload {
    public static final CustomPayload.Id<SaveBuildC2SPacket> ID = new CustomPayload.Id<>(Identifier.of(Buildnotes.MOD_ID, "save_build_c2s"));

    public static final PacketCodec<PacketByteBuf, SaveBuildC2SPacket> CODEC = CustomPayload.codecOf(
            SaveBuildC2SPacket::write,
            SaveBuildC2SPacket::new
    );

    public SaveBuildC2SPacket(PacketByteBuf buf) {
        this(Build.fromBuf(buf));
    }

    public void write(PacketByteBuf buf) { build.writeToBuf(buf); }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

