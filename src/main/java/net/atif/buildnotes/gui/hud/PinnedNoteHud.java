package net.atif.buildnotes.gui.hud;

import net.atif.buildnotes.data.ConfigManager;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.ModConfig;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PinnedNoteHud {
    // --- CACHE FIELDS ---
    private static UUID lastNoteId = null;
    private static int lastScreenWidth = -1;
    private static long lastModifiedTime = -1;
    private static float lastWidthPercent = -1f;
    private static float lastHeightPercent = -1f;
    private static Text cachedTitleText = null;
    private static List<OrderedText> cachedContentLines = new ArrayList<>();
    private static int cachedBoxHeight = 0;
    private static int cachedBoxWidth = 0;

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Hide HUD if F1 is pressed, F3 is open, or a menu is open
        if (client.options.hudHidden || client.getDebugHud().shouldShowDebugHud() || client.currentScreen != null) return;

        Note pinnedNote = DataManager.getInstance().getPinnedNote();
        if (pinnedNote == null) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        ModConfig config = ConfigManager.getConfig();

        if (!pinnedNote.getId().equals(lastNoteId) ||
                screenWidth != lastScreenWidth ||
                pinnedNote.getLastModified() != lastModifiedTime ||
                config.hudWidthPercent != lastWidthPercent ||
                config.hudHeightPercent != lastHeightPercent) {

            recalculateLayout(client, pinnedNote, screenWidth, screenHeight);
        }

        int padding = config.hudEdgePadding;
        int startX, startY;

        // X Logic
        if (config.hudPosition.contains("LEFT")) {
            startX = padding;
        } else if (config.hudPosition.contains("RIGHT")) {
            startX = screenWidth - cachedBoxWidth - padding;
        } else { // CENTER
            startX = (screenWidth / 2) - (cachedBoxWidth / 2);
        }

        // Y Logic
        if (config.hudPosition.contains("TOP")) {
            startY = padding;
        } else if (config.hudPosition.contains("BOTTOM")) {
            startY = screenHeight - cachedBoxHeight - padding;
        } else { // MIDDLE
            startY = (screenHeight / 2) - (cachedBoxHeight / 2);
        }

        // Draw Background
        context.fill(startX, startY, startX + cachedBoxWidth, startY + cachedBoxHeight, Colors.HUD_BACKGROUND);

        int innerPadding = 6;
        int currentY = startY + innerPadding;

        // Draw Cached Title
        if (cachedTitleText != null) {
            context.drawText(client.textRenderer, cachedTitleText, startX + innerPadding, currentY, 0xFFFFFFFF, true);
        }

        currentY += client.textRenderer.fontHeight + 4;
        context.fill(startX + innerPadding, currentY - 2, startX + cachedBoxWidth - innerPadding, currentY - 1, 0x55FFFFFF);
        currentY += 2;

        // Draw Cached Content
        int maxBottomY = startY + cachedBoxHeight - innerPadding;
        for (OrderedText line : cachedContentLines) {
            if (currentY + client.textRenderer.fontHeight > maxBottomY) break;
            context.drawText(client.textRenderer, line, startX + innerPadding, currentY, Colors.HUD_CONTENT, true);
            currentY += client.textRenderer.fontHeight + 2;
        }
    }

    private static void recalculateLayout(MinecraftClient client, Note note, int screenWidth, int screenHeight) {
        ModConfig config = ConfigManager.getConfig();
        int innerPadding = 6;

        // Use percentages from config
        lastWidthPercent = config.hudWidthPercent;
        lastHeightPercent = config.hudHeightPercent;
        int targetWidth = (int) (screenWidth * lastWidthPercent);
        int maxHeight = (int) (screenHeight * lastHeightPercent);

        cachedBoxWidth = Math.max(targetWidth, 120);
        int textWidth = cachedBoxWidth - (innerPadding * 2);

        // Title Trimming
        String titleStr = note.getTitle();
        if (titleStr == null || titleStr.isEmpty()) titleStr = "Untitled";
        String fullTitleStr = "📌 " + titleStr;
        Text fullTitleText = Text.literal(fullTitleStr).formatted(Formatting.GOLD, Formatting.BOLD);

        StringVisitable trimmedVisitable = client.textRenderer.trimToWidth(fullTitleText, textWidth);
        cachedTitleText = Text.literal(trimmedVisitable.getString()).formatted(Formatting.GOLD, Formatting.BOLD);

//        String trimmed = client.textRenderer.trimToWidth(fullTitleStr, textWidth - 40);
//        cachedTitleText = Text.literal(trimmed).formatted(Formatting.GOLD, Formatting.BOLD);

        // Content Wrapping
        String content = note.getContent() == null ? "" : note.getContent();
        cachedContentLines = client.textRenderer.wrapLines(StringVisitable.plain(content), textWidth);

        // Calculate Height
        int lineHeight = client.textRenderer.fontHeight + 2;
        int requiredTextHeight = client.textRenderer.fontHeight + 6 + (cachedContentLines.size() * lineHeight);
        cachedBoxHeight = Math.min(maxHeight, requiredTextHeight + (innerPadding * 2));

        // Update tracking variables
        lastNoteId = note.getId();
        lastScreenWidth = screenWidth;
        lastModifiedTime = note.getLastModified();
    }
}