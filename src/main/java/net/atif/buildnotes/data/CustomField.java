package net.atif.buildnotes.data;

import net.minecraft.network.PacketByteBuf;

public class CustomField {
    private String title;
    private String content;

    public CustomField(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeString(this.title);
        buf.writeString(this.content);
    }

    public static CustomField fromBuf(PacketByteBuf buf) {
        return new CustomField(buf.readString(), buf.readString());
    }
}