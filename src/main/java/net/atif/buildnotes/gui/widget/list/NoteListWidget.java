package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.screen.MainScreen;

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

    public NoteListWidget(MainScreen parent, MinecraftClient client, int top, int bottom, int itemHeight) {
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
            // Prepare Scope indicator to calculate its width
            Text scopeText = null;
            int scopeWidth = 0;
            if (note.getScope() != null) {
                switch (note.getScope()) {
                    case GLOBAL -> scopeText = new LiteralText("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = new LiteralText("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope to keep the UI clean
                }
            }

            if (scopeText != null) {
                scopeWidth = client.textRenderer.getWidth(scopeText);
            }

            // Truncate and draw the Title
            // Calculate available width for the title by subtracting space for the scope indicator and padding
            int availableTitleWidth = entryWidth - 4; // Base padding
            if (scopeText != null) {
                availableTitleWidth -= (scopeWidth + 7); // Account for the scope text and its padding
            }

            String truncatedTitle = client.textRenderer.trimToWidth(note.getTitle(), availableTitleWidth);
            client.textRenderer.draw(matrices, Text.of(truncatedTitle), x + 2, y + 2, Colors.TEXT_PRIMARY);

            if (scopeText != null) {
                client.textRenderer.draw(matrices, scopeText, x + entryWidth - scopeWidth - 4, y + 2, Colors.TEXT_PRIMARY);
            }

            // Truncate and draw the Content Preview
            Text contentPreview = new LiteralText(firstLine).formatted(Formatting.GRAY);
            String truncatedContent = client.textRenderer.trimToWidth(contentPreview.getString(), entryWidth - 6);
            client.textRenderer.draw(matrices, new LiteralText(truncatedContent), x + 2, y + 12, Colors.TEXT_MUTED);

            client.textRenderer.draw(matrices, Text.of("Last Modified: " + this.formattedDateTime), x + 2, y + 22, Colors.TEXT_MUTED);
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