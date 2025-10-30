package net.atif.buildnotes.network;

// This class holds constants that both the client and server need to agree on.
public class NetworkConstants {

    /**
     * The size of each image chunk in bytes.
     * A smaller size is safer for poor connections but results in more packets.
     * 24KB is a good, safe value.
     */
    public static final int CHUNK_SIZE = 24 * 1024;
}