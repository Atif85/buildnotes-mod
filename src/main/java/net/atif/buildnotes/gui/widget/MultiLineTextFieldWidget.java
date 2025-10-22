package net.atif.buildnotes.gui.widget;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class MultiLineTextFieldWidget implements Drawable, Element, Selectable {

    private final TextRenderer textRenderer;
    private final int x, y, width, height;

    private List<String> lines = Lists.newArrayList("");
    private int cursorX = 0; // column
    private int cursorY = 0; // line index
    private boolean focused = false;
    private int tickCounter = 0;
    private double scrollY = 0;

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        setText(initialText);
    }

    public void setText(String text) {
        this.lines.clear();
        if (text == null || text.isEmpty()) {
            this.lines.add("");
        } else {
            this.lines.addAll(Arrays.asList(text.split("\n")));
        }
        this.setCursorToEnd();
        this.focused = false;
    }

    public String getText() {
        return String.join("\n", this.lines);
    }

    private void setCursor(int x, int y) {
        this.cursorY = Math.max(0, Math.min(y, this.lines.size() - 1));
        this.cursorX = Math.max(0, Math.min(x, this.lines.get(this.cursorY).length()));
        ensureCursorVisible();
    }

    private void setCursorToEnd() {
        this.cursorY = this.lines.size() - 1;
        this.cursorX = this.lines.get(this.cursorY).length();
        ensureCursorVisible();
    }

    public void insertText(String text) {
        String currentLine = this.lines.get(this.cursorY);
        String beforeCursor = currentLine.substring(0, this.cursorX);
        String afterCursor = currentLine.substring(this.cursorX);

        String newText = beforeCursor + text + afterCursor;
        this.lines.set(this.cursorY, newText);
        this.cursorX += text.length();
        ensureCursorVisible();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.tickCounter++;
        // The background panel is drawn by the screen, not the widget

        int contentX = this.x + 5;
        int contentY = this.y + 5;
        int contentWidth = this.width - 10;
        int contentHeight = this.height - 10;

        // Scissor for scrolling
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int scissorX = (int) (this.x * scale);
        int scissorY = (int) (MinecraftClient.getInstance().getWindow().getFramebufferHeight() - ((this.y + this.height) * scale));
        int scissorWidth = (int) (this.width * scale);
        int scissorHeight = (int) (this.height * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        // Draw visible lines
        int firstVisibleLine = (int) (scrollY / textRenderer.fontHeight);
        int lastVisibleLine = Math.min(this.lines.size() - 1, firstVisibleLine + (contentHeight / textRenderer.fontHeight) + 1);

        for (int i = firstVisibleLine; i <= lastVisibleLine; i++) {
            int lineYPos = contentY + (i * textRenderer.fontHeight) - (int)scrollY;
            this.textRenderer.draw(matrices, this.lines.get(i), contentX, lineYPos, 0xFFFFFF);
        }

        // Draw cursor
        if (this.focused && (this.tickCounter / 6) % 2 == 0) {
            if (cursorY >= firstVisibleLine && cursorY <= lastVisibleLine) {
                String line = this.lines.get(this.cursorY);
                int cursorRenderX = contentX + this.textRenderer.getWidth(line.substring(0, this.cursorX));
                int cursorRenderY = contentY + (cursorY * textRenderer.fontHeight) - (int) scrollY;
                fill(matrices, cursorRenderX, cursorRenderY - 1, cursorRenderX + 1, cursorRenderY + this.textRenderer.fontHeight, 0xFFFFFFFF);
            }
        }

        com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            this.focused = true;

            int lineIndex = (int) ((mouseY - (this.y + 5) + scrollY) / textRenderer.fontHeight);
            lineIndex = Math.max(0, Math.min(lineIndex, this.lines.size() - 1));

            String line = this.lines.get(lineIndex);
            int charIndex = this.textRenderer.trimToWidth(line, (int) (mouseX - (this.x + 5))).length();

            setCursor(charIndex, lineIndex);

            return true;
        }
        this.focused = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY)) {
            int maxScroll = Math.max(0, (this.lines.size() * textRenderer.fontHeight) - (height - 10));
            this.scrollY -= amount * 10;
            if (this.scrollY > maxScroll) this.scrollY = maxScroll;
            if (this.scrollY < 0) this.scrollY = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) return false;

        if (Screen.isSelectAll(keyCode)) {
            // TODO: Implement selection
            return true;
        } else if (Screen.isCopy(keyCode)) {
            // TODO: Implement selection
            return true;
        } else if (Screen.isPaste(keyCode)) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            insertText(clipboard);
            return true;
        } else if (Screen.isCut(keyCode)) {
            // TODO: Implement selection
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                String currentLine = this.lines.get(this.cursorY);
                String beforeCursor = currentLine.substring(0, this.cursorX);
                String afterCursor = currentLine.substring(this.cursorX);
                this.lines.set(this.cursorY, beforeCursor);
                this.lines.add(this.cursorY + 1, afterCursor);
                setCursor(0, this.cursorY + 1);
                return true;

            case GLFW.GLFW_KEY_BACKSPACE:
                if (cursorX == 0 && cursorY > 0) {
                    // Backspace at the start of a line
                    String lineToMerge = this.lines.remove(this.cursorY);
                    int prevLineIndex = this.cursorY - 1;
                    String prevLine = this.lines.get(prevLineIndex);
                    int newCursorX = prevLine.length();
                    this.lines.set(prevLineIndex, prevLine + lineToMerge);
                    setCursor(newCursorX, prevLineIndex);
                } else if (cursorX > 0) {
                    // Backspace in the middle of a line
                    String line = this.lines.get(this.cursorY);
                    String before = line.substring(0, cursorX - 1);
                    String after = line.substring(cursorX);
                    this.lines.set(this.cursorY, before + after);
                    setCursor(cursorX - 1, cursorY);
                }
                return true;

            case GLFW.GLFW_KEY_DELETE:
                if (cursorX == this.lines.get(this.cursorY).length() && cursorY < this.lines.size() - 1) {
                    // Delete at the end of a line
                    String nextLine = this.lines.remove(cursorY + 1);
                    this.lines.set(cursorY, this.lines.get(cursorY) + nextLine);
                } else if (cursorX < this.lines.get(this.cursorY).length()) {
                    // Delete in the middle of a line
                    String line = this.lines.get(this.cursorY);
                    String before = line.substring(0, cursorX);
                    String after = line.substring(cursorX + 1);
                    this.lines.set(this.cursorY, before + after);
                }
                return true;

            case GLFW.GLFW_KEY_UP:
                setCursor(this.cursorX, this.cursorY - 1);
                return true;
            case GLFW.GLFW_KEY_DOWN:
                setCursor(this.cursorX, this.cursorY + 1);
                return true;
            case GLFW.GLFW_KEY_LEFT:
                if (cursorX == 0 && cursorY > 0) {
                    setCursor(this.lines.get(cursorY - 1).length(), cursorY - 1);
                } else {
                    setCursor(this.cursorX - 1, this.cursorY);
                }
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                if (cursorX == this.lines.get(cursorY).length() && cursorY < this.lines.size() - 1) {
                    setCursor(0, cursorY + 1);
                } else {
                    setCursor(this.cursorX + 1, this.cursorY);
                }
                return true;

            case GLFW.GLFW_KEY_HOME:
                setCursor(0, this.cursorY);
                return true;
            case GLFW.GLFW_KEY_END:
                setCursor(this.lines.get(this.cursorY).length(), this.cursorY);
                return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.focused) {
            insertText(Character.toString(chr));
            return true;
        }
        return false;
    }

    private void ensureCursorVisible() {
        // Scroll up if cursor is above the visible area
        int topVisibleLine = (int) (scrollY / textRenderer.fontHeight);
        if (cursorY < topVisibleLine) {
            scrollY = cursorY * textRenderer.fontHeight;
        }

        // Scroll down if cursor is below the visible area
        int linesOnScreen = (height - 10) / textRenderer.fontHeight;
        int bottomVisibleLine = topVisibleLine + linesOnScreen - 1;
        if (cursorY > bottomVisibleLine) {
            scrollY = (cursorY - linesOnScreen + 1) * textRenderer.fontHeight;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public SelectionType getType() {
        return this.focused ? SelectionType.FOCUSED : SelectionType.HOVERED;
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // Not needed for this mod
    }
}