package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.DataManager;
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

            boolean isPinned = build.getId().equals(DataManager.getInstance().getPinnedBuildId());

            // Prepare Base Texts
            Component scopeText = null;
            if (build.getScope() != null) {
                switch (build.getScope()) {
                    case GLOBAL -> scopeText = Component.literal("Global").withStyle(ChatFormatting.AQUA);
                    case SERVER -> scopeText = Component.literal("Server").withStyle(ChatFormatting.GREEN);
                    // We don't draw an indicator for WORLD scope
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

            // Truncate and draw the Build Name (Line 1)
            int availableNameWidth = entryWidth - 8; // Base padding
            if (line1RightText != null) {
                availableNameWidth -= (line1RightWidth + 7); // Account for Line 1 text and padding
            }
            String truncatedName = minecraft.font.plainSubstrByWidth(build.getName(), availableNameWidth);
            graphics.text(minecraft.font, truncatedName, entryX + 4, entryY + 4, Colors.TEXT_PRIMARY, false);

            if (line1RightText != null) {
                graphics.text(minecraft.font, line1RightText, entryX + entryWidth - line1RightWidth - 4, entryY + 4, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Coordinates (Line 2)
            int availableCoordsWidth = entryWidth - 8;
            if (line2RightText != null) {
                availableCoordsWidth -= (line2RightWidth + 7); // Account for Line 2 text and padding so they don't overlap
            }
            String fullCoordsText = "Coords: " + build.getCoordinates();
            String truncatedCoords = minecraft.font.plainSubstrByWidth(fullCoordsText, availableCoordsWidth);
            graphics.text(minecraft.font, Component.literal(truncatedCoords).withStyle(ChatFormatting.GRAY), entryX + 4, entryY + 14, Colors.TEXT_MUTED, false);

            if (line2RightText != null) {
                graphics.text(minecraft.font, line2RightText, entryX + entryWidth - line2RightWidth - 4, entryY + 14, Colors.TEXT_PRIMARY, false);
            }

            // Truncate and draw the Date/Time (Line 3)
            String fullDateText = "Last Modified: " + this.formattedDateTime;
            graphics.text(minecraft.font, Component.literal(fullDateText).withStyle(ChatFormatting.GRAY), entryX + 4, entryY + 24, Colors.TEXT_MUTED, false);
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