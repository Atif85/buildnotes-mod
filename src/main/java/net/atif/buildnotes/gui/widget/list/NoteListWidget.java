package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Note;
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
            this.firstLine = note.getContent().split("\n")[0];

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
            // Line 1: Title
            client.textRenderer.draw(matrices, note.getTitle(), x + 2, y + 2, 0xFFFFFF);

            // Line 2: Content Preview
            Text contentPreview = new LiteralText(firstLine).formatted(Formatting.GRAY);
            String truncated = client.textRenderer.trimToWidth(contentPreview.getString(), entryWidth - 4);
            client.textRenderer.draw(matrices, new LiteralText(truncated), x + 2, y + 12, 0xCCCCCC);

            // Line 3: Date/Time with new label
            client.textRenderer.draw(matrices, "Last Modified: " + this.formattedDateTime, x + 2, y + 22, 0xCCCCCC);

            // "Global" Indicator on the right
            if (note.isGlobal()) {
                Text globalText = new LiteralText("Global").formatted(Formatting.AQUA);
                int globalWidth = client.textRenderer.getWidth(globalText);
                client.textRenderer.draw(matrices, globalText, x + entryWidth - globalWidth - 7, y + 2, 0xFFFFFF);
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