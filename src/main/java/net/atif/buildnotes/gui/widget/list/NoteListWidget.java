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

public class NoteListWidget extends AbstractListWidget<NoteListWidget.NoteEntry> {

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

        public NoteEntry(Note note) {
            this.note = note;
            this.firstLine = note.getContent().split("\n")[0];
        }

        public Note getNote() {
            return this.note;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            client.textRenderer.draw(matrices, note.getTitle(), x + 2, y + 2, 0xFFFFFF);
            Text contentPreview = new LiteralText(firstLine).formatted(Formatting.GRAY);
            String truncated = client.textRenderer.trimToWidth(contentPreview.getString(), entryWidth - 4);
            client.textRenderer.draw(matrices, new LiteralText(truncated), x + 2, y + 14, 0xCCCCCC);
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