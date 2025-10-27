package net.atif.buildnotes.gui.helper;

import net.atif.buildnotes.gui.screen.ConfirmScreen; // Or your YesNoScreen implementation
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class UIHelper {

    public static final int MIN_BUTTON_WIDTH = 70;
    public static final int BUTTON_WIDTH = 70; // Increased from 80
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_SPACING = 8;
    public static final int BOTTOM_PADDING = 12;
    public static final int BUTTON_TEXT_PADDING = 10; // Padding for dynamically sized buttons

    /**
     * A functional interface for creating buttons with dynamic placement and size.
     */
    @FunctionalInterface
    public interface DynamicButtonCreator {
        void create(int index, int x, int width);
    }

    /**
     * Draws the standard dark, semi-transparent panel background.
     */
    public static void drawPanel(MatrixStack matrices, int x, int y, int width, int height) {
        fill(matrices, x, y, x + width, y + height, 0x77000000);
    }

    /**
     * Calculates the starting X position for a row of centered, fixed-width buttons.
     */
    public static int getCenteredButtonStartX(int screenWidth, int buttonCount) {
        int totalWidth = (buttonCount * BUTTON_WIDTH) + ((buttonCount - 1) * BUTTON_SPACING);
        return (screenWidth - totalWidth) / 2;
    }

    /**
     * [BACKWARD COMPATIBLE] Creates a row of fixed-width action buttons.
     */
    public static int createBottomButtonRow(Screen screen, int y, int buttonCount, Consumer<Integer> buttonCreator) {
        int startX = getCenteredButtonStartX(screen.width, buttonCount);
        for (int i = 0; i < buttonCount; i++) {
            int buttonX = startX + i * (BUTTON_WIDTH + BUTTON_SPACING);
            buttonCreator.accept(buttonX);
        }
        return y;
    }

    /**
     * [NEW] Creates a row of dynamically sized buttons, centered on the screen.
     */
    public static void createBottomButtonRow(Screen screen, int y, List<Text> buttonTexts, DynamicButtonCreator creator) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (buttonTexts.isEmpty()) return;

        // Calculate total width of all buttons
        int[] widths = new int[buttonTexts.size()];
        int totalWidth = 0;
        for (int i = 0; i < buttonTexts.size(); i++) {
            // Calculate the width based on text, but ensure it's at least BUTTON_WIDTH.
            int textWidth = textRenderer.getWidth(buttonTexts.get(i)) + BUTTON_TEXT_PADDING * 2;
            widths[i] = Math.max(textWidth, BUTTON_WIDTH);
            totalWidth += widths[i];
        }
        totalWidth += BUTTON_SPACING * (buttonTexts.size() - 1);

        // Calculate starting position to center the row
        int currentX = (screen.width - totalWidth) / 2;

        // Create and place each button
        for (int i = 0; i < buttonTexts.size(); i++) {
            creator.create(i, currentX, widths[i]);
            currentX += widths[i] + BUTTON_SPACING;
        }
    }

    /**
     * Shortcut to open a standard confirm dialog.
     */
    public static void showConfirmDialog(Screen parent, Text message, Runnable onConfirm) {
        MinecraftClient.getInstance().setScreen(new ConfirmScreen(parent, message, onConfirm, () -> MinecraftClient.getInstance().setScreen(parent)));
    }
}