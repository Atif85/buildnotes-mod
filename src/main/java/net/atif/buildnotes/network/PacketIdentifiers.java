package net.atif.buildnotes.network;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.util.Identifier;

public class PacketIdentifiers {
    // --- Server to Client (S2C) ---
    public static final Identifier HANDSHAKE_S2C = new Identifier(Buildnotes.MOD_ID, "handshake_s2c");
    public static final Identifier INITIAL_SYNC_S2C = new Identifier(Buildnotes.MOD_ID, "initial_sync_s2c");
    public static final Identifier UPDATE_NOTE_S2C = new Identifier(Buildnotes.MOD_ID, "update_note_s2c");
    public static final Identifier UPDATE_BUILD_S2C = new Identifier(Buildnotes.MOD_ID, "update_build_s2c");
    public static final Identifier DELETE_NOTE_S2C = new Identifier(Buildnotes.MOD_ID, "delete_note_s2c");
    public static final Identifier DELETE_BUILD_S2C = new Identifier(Buildnotes.MOD_ID, "delete_build_s2c");
    public static final Identifier UPDATE_PERMISSION_S2C = new Identifier(Buildnotes.MOD_ID, "update_permission_s2c");


    // --- Client to Server (C2S) ---
    public static final Identifier REQUEST_DATA_C2S = new Identifier(Buildnotes.MOD_ID, "request_data_c2s");
    public static final Identifier SAVE_NOTE_C2S = new Identifier(Buildnotes.MOD_ID, "save_note_c2s");
    public static final Identifier SAVE_BUILD_C2S = new Identifier(Buildnotes.MOD_ID, "save_build_c2s");
    public static final Identifier DELETE_NOTE_C2S = new Identifier(Buildnotes.MOD_ID, "delete_note_c2s");
    public static final Identifier DELETE_BUILD_C2S = new Identifier(Buildnotes.MOD_ID, "delete_build_c2s");

    // Image Transfer Packets
    public static final Identifier UPLOAD_IMAGE_CHUNK_C2S = new Identifier(Buildnotes.MOD_ID, "upload_image_chunk_c2s");
    public static final Identifier REQUEST_IMAGE_C2S = new Identifier(Buildnotes.MOD_ID, "request_image_c2s");
    public static final Identifier IMAGE_CHUNK_S2C = new Identifier(Buildnotes.MOD_ID, "image_chunk_s2c");
    public static final Identifier IMAGE_NOT_FOUND_S2C = new Identifier(Buildnotes.MOD_ID, "image_not_found_s2c");
}