package net.atif.buildnotes.data;

import java.time.Instant;
import java.util.UUID;

public class Note {
    private final UUID id;
    private String title;
    private String content;
    private long lastModified;

    public Note(String title, String content) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.content = content;
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

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }
}