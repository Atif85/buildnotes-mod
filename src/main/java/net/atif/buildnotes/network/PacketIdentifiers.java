package net.atif.buildnotes.network;

import net.atif.buildnotes.Buildnotes;
import net.minecraft.util.Identifier;

public class PacketIdentifiers {
    // Server to Client
    public static final Identifier HANDSHAKE_S2C = new Identifier(Buildnotes.MOD_ID, "handshake_s2c");

    // Client to Server
    public static final Identifier REQUEST_DATA_C2S = new Identifier(Buildnotes.MOD_ID, "request_data_c2s");
}