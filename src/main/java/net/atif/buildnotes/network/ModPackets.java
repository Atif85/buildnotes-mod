package net.atif.buildnotes.network;

import net.atif.buildnotes.network.packet.c2s.*;
import net.atif.buildnotes.network.packet.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class ModPackets {

    /**
     * Registers all Client-to-Server (C2S) packet types.
     * This should be called from your main ModInitializer.
     */
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(DeleteNoteC2SPacket.ID, DeleteNoteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestDataC2SPacket.ID, RequestDataC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveNoteC2SPacket.ID, SaveNoteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveBuildC2SPacket.ID, SaveBuildC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteBuildC2SPacket.ID, DeleteBuildC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadImageChunkC2SPacket.ID, UploadImageChunkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestImageC2SPacket.ID, RequestImageC2SPacket.CODEC);
    }

    /**
     * Registers all Server-to-Client (S2C) packet types.
     * This should be called from your ClientModInitializer.
     */
    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(HandshakeS2CPacket.ID, HandshakeS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(InitialSyncS2CPacket.ID, InitialSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateNoteS2CPacket.ID, UpdateNoteS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(UpdateBuildS2CPacket.ID, UpdateBuildS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DeleteNoteS2CPacket.ID, DeleteNoteS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(DeleteBuildS2CPacket.ID, DeleteBuildS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageChunkS2CPacket.ID, ImageChunkS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ImageNotFoundS2CPacket.ID, ImageNotFoundS2CPacket.CODEC);
    }
}