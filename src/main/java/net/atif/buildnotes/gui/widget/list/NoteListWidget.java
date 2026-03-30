package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class NoteListWidget extends AbstractListWidget<NoteListWidget.NoteEntry> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public NoteListWidget(MainScreen parent, Minecraft client, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setNotes(List<Note> notes) {
        this.clearEntries();
        notes.forEach(note -> this.addEntry(new NoteEntry(note)));
    }

    public Note getSelectedNote() {
        NoteEntry entry = getSelected();
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int entryX = getX();
            int entryY = getY();
            int entryWidth = NoteListWidget.this.getRowWidth();

            boolean isPinned = note.getId().equals(DataManager.getInstance().getPinnedNoteId());

            // Prepare Base Texts
            Component scopeText = null;
            if (note.getScope() != null) {
                switch (note.getScope()) {
                    case GLOBAL -> scopeText = Component.literal("Global").withStyle(ChatFormatting.AQUA);
                    case SERVER -> scopeText = Component.literal("Server").withStyle(ChatFormatting.GREEN);
                    // We don't draw an indicator for WORLD scope to keep the UI clean
                }
            }
            Component pinText = isPinned ? Component.literal("📌 Pinned").withStyle(ChatFormatting.GOLD) : null;

            // Figure out layout positions
            Component line1RightText = null;
            Component line2RightText = null;

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
            int line1RightWidth = line1RightText != null ? minecraft.font.width(line1RightText) : 0;
            int line2RightWidth = line2RightText != null ? minecraft.font.width(line2RightText) : 0;

            // Truncate and draw the Title (Line 1)
            int availableTitleWidth = entryWidth - 8; // Base padding
            if (line1RightText != null) {
                availableTitleWidth -= (line1RightWidth + 7); // Account for Line 1 text and padding
            }
            String truncatedTitle = minecraft.font.plainSubstrByWidth(note.getTitle(), availableTitleWidth);
            graphics.text(minecraft.font, truncatedTitle, entryX + 4, entryY + 4, Colors.TEXT_PRIMARY, false);

            if (line1RightText != null) {
                graphics.text(minecraft.font, line1RightText, entryX + entryWidth - line1RightWidth - 4, entryY + 4, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Content Preview (Line 2)
            int availableContentWidth = entryWidth - 8;
            if (line2RightText != null) {
                availableContentWidth -= (line2RightWidth + 7); // Account for Line 2 text and padding so they don't overlap
            }
            String truncatedContent = minecraft.font.plainSubstrByWidth(firstLine, availableContentWidth);
            graphics.text(minecraft.font, Component.literal(truncatedContent).withStyle(ChatFormatting.GRAY), entryX + 4, entryY + 14, Colors.TEXT_MUTED, false);

            if (line2RightText != null) {
                graphics.text(minecraft.font, line2RightText, entryX + entryWidth - line2RightWidth - 4, entryY + 14, Colors.TEXT_PRIMARY, false);
            }

            // Draw Last Modified (Line 3)
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            graphics.text(minecraft.font, Component.literal(fullDateText).withStyle(ChatFormatting.GRAY), entryX + 4, entryY + 24, Colors.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) {
                NoteListWidget.this.setSelected(this);

                NoteListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("gui.narrate.entry", note.getTitle());
        }
    }
}