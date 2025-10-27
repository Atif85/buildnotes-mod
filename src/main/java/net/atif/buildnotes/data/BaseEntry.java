package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.UUID;

/**
 * Common base for data types that share id/lastModified/global scope fields.
 */
public abstract class BaseEntry {
    private final UUID id;
    private long lastModified;
    private boolean isGlobal;

    /**
     * Default constructor (used by code and deserializers).
     * Assigns a new random id and sets the initial timestamp.
     */
    protected BaseEntry() {
        this.id = UUID.randomUUID();
        this.isGlobal = false;
        this.updateTimestamp();
    }

    /**
     * Update the last-modified timestamp to now (epoch seconds).
     */
    public void updateTimestamp() {
        this.lastModified = Instant.now().getEpochSecond();
    }

    // --- Common getters / setters ---
    public UUID getId() {
        return id;
    }

    public long getLastModified() {
        return lastModified;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        this.isGlobal = global;
    }
}
