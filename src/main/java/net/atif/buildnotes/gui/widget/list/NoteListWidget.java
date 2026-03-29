package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
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

    public class NoteEntry extends AbstractListWidget.Entry<NoteEntry> {
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
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int entryX = getX();
            int entryY = getY();
            int entryWidth = NoteListWidget.this.getRowWidth();

            boolean isPinned = note.getId().equals(DataManager.getInstance().getPinnedNoteId());

            // Prepare Base Texts
            Text scopeText = null;
            if (note.getScope() != null) {
                switch (note.getScope()) {
                    case GLOBAL -> scopeText = Text.literal("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = Text.literal("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope to keep the UI clean
                }
            }
            Text pinText = isPinned ? Text.literal("📌 Pinned").formatted(Formatting.GOLD) : null;

            // Figure out layout positions
            Text line1RightText = null;
            Text line2RightText = null;

            if (pinText != null) {
                // If there is a pinned, Pinned gets Line 1, Scope gets pushed to Line 2
                line1RightText = pinText;
                if (scopeText != null) {
                    line2RightText = scopeText;
                }
            } else {
                // If no pinned, Scope takes Line 1
                if (scopeText != null) {
                    line1RightText = scopeText;
                }
            }

            // Calculate widths for truncating
            int line1RightWidth = line1RightText != null ? client.textRenderer.getWidth(line1RightText) : 0;
            int line2RightWidth = line2RightText != null ? client.textRenderer.getWidth(line2RightText) : 0;

            // Truncate and draw the Title (Line 1)
            int availableTitleWidth = entryWidth - 8; // Base padding
            if (line1RightText != null) {
                availableTitleWidth -= (line1RightWidth + 7); // Account for Line 1 text and padding
            }
            String truncatedTitle = client.textRenderer.trimToWidth(note.getTitle(), availableTitleWidth);
            context.drawText(client.textRenderer, truncatedTitle, entryX + 4, entryY + 4, Colors.TEXT_PRIMARY, false);

            if (line1RightText != null) {
                context.drawText(client.textRenderer, line1RightText, entryX + entryWidth - line1RightWidth - 4, entryY + 4, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Content Preview (Line 2)
            int availableContentWidth = entryWidth - 8;
            if (line2RightText != null) {
                availableContentWidth -= (line2RightWidth + 7); // Account for Line 2 text and padding so they don't overlap
            }
            String truncatedContent = client.textRenderer.trimToWidth(firstLine, availableContentWidth);
            context.drawText(client.textRenderer, Text.literal(truncatedContent).formatted(Formatting.GRAY), entryX + 4, entryY + 14, Colors.TEXT_MUTED, false);

            if (line2RightText != null) {
                context.drawText(client.textRenderer, line2RightText, entryX + entryWidth - line2RightWidth - 4, entryY + 14, Colors.TEXT_PRIMARY, false);
            }

            // Draw Last Modified (Line 3)
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            context.drawText(client.textRenderer, Text.literal(fullDateText).formatted(Formatting.GRAY), entryX + 4, entryY + 24, Colors.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() == 0) {
                NoteListWidget.this.setSelected(this);

                NoteListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }
    }
}