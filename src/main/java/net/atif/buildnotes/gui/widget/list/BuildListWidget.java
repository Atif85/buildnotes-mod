package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    public class BuildEntry extends AbstractListWidget.Entry<BuildEntry> {
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            // Line 1: Name
            context.drawText(client.textRenderer, build.getName(), x + 2, y + 2, 0xFFFFFF, false);

            // Line 2: Coords
            Text coords = Text.literal("Coords: " + build.getCoordinates()).formatted(Formatting.GRAY);
            context.drawText(client.textRenderer, coords, x + 2, y + 12, 0xCCCCCC, false);

            // Line 3: Date/Time with new label
            context.drawText(client.textRenderer, "Last Modified: " + this.formattedDateTime, x + 2, y + 22, 0xCCCCCC, false);

            // --- SCOPE INDICATOR ---
            Text scopeText = null;
            if (build.getScope() != null) { // Add null check for safety
                switch (build.getScope()) {
                    case GLOBAL -> scopeText = Text.literal("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = Text.literal("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope
                }
            }

            if (scopeText != null) {
                int scopeWidth = client.textRenderer.getWidth(scopeText);
                context.drawText(client.textRenderer, scopeText, x + entryWidth - scopeWidth - 7, y + 2, 0xFFFFFF, false);
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