package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEntry {

    private final UUID id;
    private long lastModified;
    private Scope scope;

    protected BaseEntry() {
        this.id = UUID.randomUUID();
        this.lastModified = Instant.now().getEpochSecond();
        this.scope = Scope.WORLD; // Default to WORLD scope
    }

    public UUID getId() {
        return id;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void updateTimestamp() {
        this.lastModified = Instant.now().getEpochSecond();
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

}