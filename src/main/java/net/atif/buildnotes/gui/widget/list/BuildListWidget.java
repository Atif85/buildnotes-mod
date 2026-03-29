package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.DataManager;
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

            boolean isPinned = build.getId().equals(DataManager.getInstance().getPinnedBuildId());

            // Prepare Base Texts
            Text scopeText = null;
            if (build.getScope() != null) {
                switch (build.getScope()) {
                    case GLOBAL -> scopeText = Text.literal("Global").formatted(Formatting.AQUA);
                    case SERVER -> scopeText = Text.literal("Server").formatted(Formatting.GREEN);
                    // We don't draw an indicator for WORLD scope
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

            // Truncate and draw the Build Name (Line 1)
            int availableNameWidth = entryWidth - 8; // Base padding
            if (line1RightText != null) {
                availableNameWidth -= (line1RightWidth + 7); // Account for Line 1 text and padding
            }
            String truncatedName = client.textRenderer.trimToWidth(build.getName(), availableNameWidth);
            context.drawText(client.textRenderer, truncatedName, entryX + 4, entryY + 4, Colors.TEXT_PRIMARY, false);

            if (line1RightText != null) {
                context.drawText(client.textRenderer, line1RightText, entryX + entryWidth - line1RightWidth - 4, entryY + 4, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Coordinates (Line 2)
            int availableCoordsWidth = entryWidth - 8;
            if (line2RightText != null) {
                availableCoordsWidth -= (line2RightWidth + 7); // Account for Line 2 text and padding so they don't overlap
            }
            String fullCoordsText = "Coords: " + build.getCoordinates();
            String truncatedCoords = client.textRenderer.trimToWidth(fullCoordsText, availableCoordsWidth);
            context.drawText(client.textRenderer, Text.literal(truncatedCoords).formatted(Formatting.GRAY), entryX + 4, entryY + 14, Colors.TEXT_MUTED, false);

            if (line2RightText != null) {
                context.drawText(client.textRenderer, line2RightText, entryX + entryWidth - line2RightWidth - 4, entryY + 14, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Date/Time (Line 3)
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            context.drawText(client.textRenderer, Text.literal(fullDateText).formatted(Formatting.GRAY), entryX + 4, entryY + 24, Colors.TEXT_MUTED, false);
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