package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.gui.helper.Colors;
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

public class BuildListWidget extends AbstractListWidget<BuildListWidget.BuildEntry> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public BuildListWidget(MainScreen parent, Minecraft client, int top, int bottom, int itemHeight) {
        super(parent, client, top, bottom, itemHeight);
    }

    public void setBuilds(List<Build> builds) {
        this.clearEntries();
        builds.forEach(build -> this.addEntry(new BuildEntry(build)));
    }

    public Build getSelectedBuild() {
        BuildEntry entry = getSelected();
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            int entryX = getX();
            int entryY = getY();
            int entryWidth = BuildListWidget.this.getRowWidth();

            // Prepare Scope indicator to calculate its width
            Component scopeComponent = null;
            int scopeWidth = 0;
            if (build.getScope() != null) {
                switch (build.getScope()) {
                    case GLOBAL -> scopeComponent = Component.literal("Global").withStyle(ChatFormatting.AQUA);
                    case SERVER -> scopeComponent = Component.literal("Server").withStyle(ChatFormatting.GREEN);
                    // We don't draw an indicator for WORLD scope
                }
            }

            if (scopeComponent != null) {
                scopeWidth = minecraft.font.width(scopeComponent);
            }

            // Truncate and draw the Build Name
            // Calculate available width for the name by subtracting space for the scope indicator and padding
            int availableNameWidth = entryWidth - 8; // Base padding
            if (scopeComponent != null) {
                availableNameWidth -= (scopeWidth + 7); // Account for the scope text and its padding
            }

            String truncatedName = minecraft.font.plainSubstrByWidth(build.getName(), availableNameWidth);
            graphics.text(minecraft.font, truncatedName, entryX + 4, entryY + 4, Colors.TEXT_PRIMARY, false);

            // Draw the Scope indicator
            if (scopeComponent != null) {
                graphics.text(minecraft.font, scopeComponent, entryX + entryWidth - scopeWidth - 4, entryY + 4, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Coordinates
            String fullCoordsText = "Coords: " + build.getCoordinates();
            String truncatedCoords = minecraft.font.plainSubstrByWidth(fullCoordsText, entryWidth - 8);
            graphics.text(minecraft.font, Component.literal(truncatedCoords).withStyle(ChatFormatting.GRAY), entryX + 4, entryY + 14, Colors.TEXT_MUTED, false);

            // Truncate and draw the Date/Time
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            String truncatedDate = minecraft.font.plainSubstrByWidth(fullDateText, entryWidth - 8);
            graphics.text(minecraft.font, truncatedDate, entryX + 4, entryY + 24, Colors.TEXT_MUTED, false);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            if (event.button() == 0) { // Check for left-click
                BuildListWidget.this.setSelected(this);

                BuildListWidget.this.handleEntryClick(this);
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("gui.narrate.entry", build.getName());
        }
    }
}