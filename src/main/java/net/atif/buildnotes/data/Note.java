package net.atif.buildnotes.data;

import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class Note extends BaseEntry {
    private String title;
    private String content;

    // Keep the main constructor you already had
    public Note(String title, String content) {
        super();
        this.title = title;
        this.content = content;
    }

    private Note(UUID id, long lastModified, Scope scope, String title, String content) {
        super(id, lastModified, scope); // Passes the existing data up
        this.title = title;
        this.content = content;
    }

    // Getters
    public String getTitle() { return title; }
    public String getContent() { return content; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(this.getId());
        buf.writeLong(this.getLastModified());
        buf.writeEnumConstant(this.getScope());
        buf.writeString(this.title);
        buf.writeString(this.content);
    }

    public static Note fromBuf(PacketByteBuf buf) {
        UUID id = buf.readUuid();
        long lastModified = buf.readLong();
        Scope scope = buf.readEnumConstant(Scope.class);
        String title = buf.readString();
        String content = buf.readString();

        return new Note(id, lastModified, scope, title, content);
    }
}
