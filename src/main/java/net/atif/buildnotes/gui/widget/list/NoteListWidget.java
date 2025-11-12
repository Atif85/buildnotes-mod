package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.atif.buildnotes.data.Scope;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NoteListWidget extends AbstractListWidget<NoteListWidget.NoteEntry> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NoteListWidget(MainScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setNotes(List<Note> notes) {
        this.clearEntries();
        notes.forEach(note -> this.addEntry(new NoteEntry(note)));
    }

    public Note getSelectedNote() {
            NoteEntry entry = getSelectedOrNull();
        return entry != null ? entry.getNote() : null;
    }

    public NoteEntry getPublicEntry(int index) {
        return this.getEntry(index);
    }

    @Override
    public void setSelected(NoteEntry entry) {
        super.setSelected(entry);
        parentScreen.onNoteSelected(); // Notify the parent screen
    }

    public class NoteEntry extends EntryListWidget.Entry<NoteEntry> {
        private final Note note;
        private final String firstLine;
        private final String formattedDateTime;

        public NoteEntry(Note note) {
            this.note = note;
            String content = note.getContent();
            // Check if the content is null or just an empty string
            if (content == null) {
                this.firstLine = "";
            } else {
                // Split the content into lines.
                String[] lines = content.split("\n");
                // THE CRUCIAL CHECK: Make sure the resulting array is not empty.
                if (lines.length > 0) {
                    this.firstLine = lines[0];
                } else {
                    // This handles cases like "" or "\n\n" which result in an empty array.
                    this.firstLine = "";
                }
            }

            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(note.getLastModified()), ZoneId.systemDefault()
            );

            this.formattedDateTime = dateTime.format(DATE_TIME_FORMATTER);
        }

        public Note getNote() {
            return this.note;
        }


        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // Title
            client.textRenderer.draw(matrices, note.getTitle(), x + 2, y + 2, 0xFFFFFF);

            // Content Preview
            Text contentPreview = new LiteralText(firstLine).formatted(Formatting.GRAY);
            String truncated = client.textRenderer.trimToWidth(contentPreview.getString(), entryWidth - 4);
            client.textRenderer.draw(matrices, new LiteralText(truncated), x + 2, y + 12, 0xCCCCCC);

            // Date/Time with new label
            client.textRenderer.draw(matrices, "Last Modified: " + this.formattedDateTime, x + 2, y + 22, 0xCCCCCC);

            Text scopeText = null;
            if (note.getScope() != null) { // Add null check for safety
                switch (note.getScope()) {
                    case GLOBAL -> scopeText = new LiteralText("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = new LiteralText("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope to keep the UI clean
                }
            }

            if (scopeText != null) {
                int scopeWidth = client.textRenderer.getWidth(scopeText);
                client.textRenderer.draw(matrices, scopeText, x + entryWidth - scopeWidth - 7, y + 2, 0xFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                NoteListWidget.this.setSelected(this);

                NoteListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }
    }
}