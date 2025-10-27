package net.atif.buildnotes.gui.helper;

import net.atif.buildnotes.gui.screen.ConfirmScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.function.Consumer;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class UIHelper {

    public static final int BUTTON_WIDTH = 80;
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_SPACING = 8;
    public static final int BOTTOM_PADDING = 12;

    /**
     * Draws the standard dark, semi-transparent panel background.
     */
    public static void drawPanel(MatrixStack matrices, int x, int y, int width, int height) {
        fill(matrices, x, y, x + width, y + height, 0x77000000);
    }

    /**
     * Calculates the starting X position for a row of centered buttons.
     */
    public static int getCenteredButtonStartX(int screenWidth, int buttonCount) {
        int totalWidth = (buttonCount * BUTTON_WIDTH) + ((buttonCount - 1) * BUTTON_SPACING);
        return (screenWidth - totalWidth) / 2;
    }

    /**
     * A helper for creating and adding a row of action buttons at the given Y.
     * The consumer receives the computed X coordinate for each button and should
     * create/add the button there (like screen.addDrawableChild(new DarkButtonWidget(...))).
     *
     * Returns the Y passed in (convenience).
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
     * Shortcut to open a standard confirm dialog that returns to the given parent on cancel.
     */
    public static void showConfirmDialog(Screen parent, Text message, Runnable onConfirm) {
        MinecraftClient.getInstance().setScreen(new ConfirmScreen(parent, message, onConfirm, () -> MinecraftClient.getInstance().setScreen(parent)));
    }

    /**
     * Computes a centered panel rectangle. Returns int[] { x, y, width, height }.
     * maxWidth is the maximum allowed panel width (we clamp to screenWidth - 80 like other screens).
     */
    public static int[] getCenteredPanel(int screenWidth, int screenHeight, int maxWidth, int panelHeight) {
        int panelW = Math.min(screenWidth - 80, maxWidth);
        int x = (screenWidth - panelW) / 2;
        int y = (screenHeight - panelHeight) / 2;
        return new int[] { x, y, panelW, panelHeight };
    }
}
