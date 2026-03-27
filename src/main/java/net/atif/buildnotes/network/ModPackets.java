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
        PayloadTypeRegistry.serverboundPlay().register(DeleteNoteC2SPacket.TYPE, DeleteNoteC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestDataC2SPacket.TYPE, RequestDataC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SaveNoteC2SPacket.TYPE, SaveNoteC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SaveBuildC2SPacket.TYPE, SaveBuildC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteBuildC2SPacket.TYPE, DeleteBuildC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UploadImageChunkC2SPacket.TYPE, UploadImageChunkC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestImageC2SPacket.TYPE, RequestImageC2SPacket.CODEC);
    }

    /**
     * Registers all Server-to-Client (S2C) packet types.
     * This should be called from your ClientModInitializer.
     */
    public static void registerS2CPackets() {
        PayloadTypeRegistry.clientboundPlay().register(HandshakeS2CPacket.TYPE, HandshakeS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InitialSyncS2CPacket.TYPE, InitialSyncS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdatePermissionS2CPacket.TYPE, UpdatePermissionS2CPacket.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(UpdateNoteS2CPacket.TYPE, UpdateNoteS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UpdateBuildS2CPacket.TYPE, UpdateBuildS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeleteNoteS2CPacket.TYPE, DeleteNoteS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeleteBuildS2CPacket.TYPE, DeleteBuildS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ImageChunkS2CPacket.TYPE, ImageChunkS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ImageNotFoundS2CPacket.TYPE, ImageNotFoundS2CPacket.CODEC);
    }
}