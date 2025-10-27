package net.atif.buildnotes.data;

public class Note extends BaseEntry {
    private String title;
    private String content;

    // Keep the main constructor you already had
    public Note(String title, String content) {
        super();
        this.title = title;
        this.content = content;
    }

    // Optional protected no-arg constructor for deserializers (safe)
    protected Note() {
        super();
    }

    // Getters
    public String getTitle() { return title; }
    public String getContent() { return content; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
}
