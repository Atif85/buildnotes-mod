package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.UUID;

public class Note {
    private final UUID id;
    private String title;
    private String content;
    private long lastModified;
    private boolean isGlobal;

    public Note(String title, String content) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.content = content;
        this.isGlobal = false;
        this.updateTimestamp();
    }

    public void updateTimestamp() {
        this.lastModified = Instant.now().getEpochSecond();
    }

    // Getters
    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public long getLastModified() { return lastModified; }
    public boolean isGlobal() { return isGlobal; }

    // Setters
    public void setTitle(String title) {

        this.title = title;
    }

    public void setContent(String content) {

        this.content = content;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }
}