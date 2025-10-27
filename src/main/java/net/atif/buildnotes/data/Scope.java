package net.atif.buildnotes.data;

public enum Scope {
    WORLD,   // Saved per-world (or per-server on client)
    GLOBAL,  // Saved globally on the client
    SERVER   // Saved and managed by the server
}