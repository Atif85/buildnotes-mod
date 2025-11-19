package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BuildListWidget extends AbstractListWidget<BuildListWidget.BuildEntry> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public BuildListWidget(MainScreen parent, MinecraftClient client, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setBuilds(List<Build> builds) {
        this.clearEntries();
        builds.forEach(build -> this.addEntry(new BuildEntry(build)));
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
            // Prepare Scope indicator to calculate its width
            Text scopeText = null;
            int scopeWidth = 0;
            if (build.getScope() != null) {
                switch (build.getScope()) {
                    case GLOBAL -> scopeText = Text.literal("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = Text.literal("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope
                }
            }

            if (scopeText != null) {
                scopeWidth = client.textRenderer.getWidth(scopeText);
            }

            // Truncate and draw the Build Name
            // Calculate available width for the name by subtracting space for the scope indicator and padding
            int availableNameWidth = entryWidth - 4; // Base padding
            if (scopeText != null) {
                availableNameWidth -= (scopeWidth + 7); // Account for the scope text and its padding
            }

            String truncatedName = client.textRenderer.trimToWidth(build.getName(), availableNameWidth);
            client.textRenderer.draw(matrices, truncatedName, x + 2, y + 2, Colors.TEXT_PRIMARY);

            // Draw the Scope indicator
            if (scopeText != null) {
                client.textRenderer.draw(matrices, scopeText, x + entryWidth - scopeWidth - 4, y + 2, Colors.TEXT_PRIMARY);
            }

            // Truncate and draw the Coordinates
            String fullCoordsText = "Coords: " + build.getCoordinates();
            String truncatedCoords = client.textRenderer.trimToWidth(fullCoordsText, entryWidth - 6);
            client.textRenderer.draw(matrices, Text.literal(truncatedCoords).formatted(Formatting.GRAY), x + 2, y + 12, Colors.TEXT_MUTED);

            // Truncate and draw the Date/Time
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            String truncatedDate = client.textRenderer.trimToWidth(fullDateText, entryWidth - 6);
            client.textRenderer.draw(matrices, truncatedDate, x + 2, y + 22, Colors.TEXT_MUTED);
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