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


    // --- Client to Server (C2S) ---
    public static final Identifier REQUEST_DATA_C2S = new Identifier(Buildnotes.MOD_ID, "request_data_c2s");
    public static final Identifier SAVE_NOTE_C2S = new Identifier(Buildnotes.MOD_ID, "save_note_c2s");
    public static final Identifier SAVE_BUILD_C2S = new Identifier(Buildnotes.MOD_ID, "save_build_c2s");
    public static final Identifier DELETE_NOTE_C2S = new Identifier(Buildnotes.MOD_ID, "delete_note_c2s");
    public static final Identifier DELETE_BUILD_C2S = new Identifier(Buildnotes.MOD_ID, "delete_build_c2s");


    // Image Transfer Packets
    // Client to Server: Here is the data for an image I want to save.
    public static final Identifier UPLOAD_IMAGE_C2S = new Identifier(Buildnotes.MOD_ID, "upload_image_c2s");
    // Client to Server: I don't have this image, please send it to me.
    public static final Identifier REQUEST_IMAGE_C2S = new Identifier(Buildnotes.MOD_ID, "request_image_c2s");
    // Server to Client: Here is the image data you requested.
    public static final Identifier IMAGE_DATA_S2C = new Identifier(Buildnotes.MOD_ID, "image_data_s2c");
}