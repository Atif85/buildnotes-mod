package net.atif.buildnotes.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;

import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class ReadOnlyMultiLineTextFieldWidget extends MultiLineTextFieldWidget {

    public ReadOnlyMultiLineTextFieldWidget(Font textRenderer, int x, int y, int width, int height, String initialText, int maxLines, boolean scrollingEnabled) {
        super(textRenderer, x, y, width, height, initialText, "", maxLines, scrollingEnabled);

        this.setCaretEnabled(false);
    }

    /**
     * Block all character input. This is a read-only field.
     */
    @Override
    public boolean charTyped(CharacterEvent event) {
        return false;
    }

    /**
     * Block any programmatic text insertion.
     */
    @Override
    public void insertText(String text) {
        // Do nothing.
    }

    /**
     * Block all key presses that could modify the text.
     * Allows only navigation (arrows, home, end), copying, and selecting all.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        // These keys modify text, so we block them by "handling" the event (returning true)
        // but performing no action.
        int keyCode = event.key();
        if (event.isPaste() || event.isCut() || event.isConfirmation() ||
                keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            return true;
        }

        // For all other keys (which include navigation, copy, and select all),
        // we let the parent widget handle them as usual.
        return super.keyPressed(event);
    }
}