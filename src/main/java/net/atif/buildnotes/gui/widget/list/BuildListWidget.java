package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
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
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int entryX = getX();
            int entryY = getY();
            int entryWidth = BuildListWidget.this.getRowWidth();
            
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
            context.drawText(client.textRenderer, truncatedName, entryX + 2, entryY + 2, Colors.TEXT_PRIMARY, false);

            // Draw the Scope indicator
            if (scopeText != null) {
                context.drawText(client.textRenderer, scopeText, entryX + entryWidth - scopeWidth - 7, entryY + 2, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Coordinates
            String fullCoordsText = "Coords: " + build.getCoordinates();
            String truncatedCoords = client.textRenderer.trimToWidth(fullCoordsText, entryWidth - 4);
            context.drawText(client.textRenderer, Text.literal(truncatedCoords).formatted(Formatting.GRAY), entryX + 2, entryY + 12, Colors.TEXT_MUTED, false);

            // Truncate and draw the Date/Time
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            String truncatedDate = client.textRenderer.trimToWidth(fullDateText, entryWidth - 4);
            context.drawText(client.textRenderer, truncatedDate, entryX + 2, entryY + 22, Colors.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (click.button() == 0) { // Check for left-click
                BuildListWidget.this.setSelected(this);

                BuildListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }
    }
}