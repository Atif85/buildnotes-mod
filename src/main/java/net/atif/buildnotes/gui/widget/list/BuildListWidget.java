package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
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

public class BuildListWidget extends AbstractListWidget<BuildListWidget.BuildEntry> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public BuildListWidget(MainScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setBuilds(List<Build> builds) {
        this.clearEntries();
        builds.forEach(build -> this.addEntry(new BuildEntry(build)));
    }

    public BuildEntry getPublicEntry(int index) {
        return this.getEntry(index);
    }

    public Build getSelectedBuild() {
        BuildEntry entry = getSelectedOrNull();
        return entry != null ? entry.getBuild() : null;
    }

    @Override
    public void setSelected(BuildEntry entry) {
        super.setSelected(entry);
        parentScreen.onBuildSelected(); // Notify the parent screen
    }

    public class BuildEntry extends EntryListWidget.Entry<BuildEntry> {
        private final Build build;
        private final String formattedDateTime;

        public BuildEntry(Build build) {
            this.build = build;

            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(build.getLastModified()), ZoneId.systemDefault());
            this.formattedDateTime = dateTime.format(DATE_TIME_FORMATTER);
        }

        public Build getBuild() {
            return this.build;
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // Line 1: Name
            client.textRenderer.draw(matrices, build.getName(), x + 2, y + 2, 0xFFFFFF);

            // Line 2: Coords
            Text coords = new LiteralText("Coords: " + build.getCoordinates()).formatted(Formatting.GRAY);
            client.textRenderer.draw(matrices, coords, x + 2, y + 12, 0xCCCCCC);

            // Line 3: Date/Time with new label
            client.textRenderer.draw(matrices, "Last Modified: " + this.formattedDateTime, x + 2, y + 22, 0xCCCCCC);

            // "Global" Indicator on the right
            if (build.isGlobal()) {
                Text globalText = new LiteralText("Global").formatted(Formatting.AQUA);
                int globalWidth = client.textRenderer.getWidth(globalText);
                client.textRenderer.draw(matrices, globalText, x + entryWidth - globalWidth - 7, y + 2, 0xFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                BuildListWidget.this.setSelected(this);

                BuildListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }
    }
}