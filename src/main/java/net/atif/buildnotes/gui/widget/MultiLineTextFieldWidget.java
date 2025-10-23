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
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class MultiLineTextFieldWidget implements Drawable, Element, Selectable {

    private final TextRenderer textRenderer;
    private final int x, y, width, height;
    private final int maxLines;
    private final boolean scrollingEnabled;

    private List<String> lines = Lists.newArrayList("");
    private int cursorX = 0; // column
    private int cursorY = 0; // line index
    private boolean focused = false;
    private float cursorBlinkTimer = 0;
    private double scrollY = 0;
    private double scrollX = 0;

    private static final int SCROLLBAR_WIDTH = 6;
    private boolean isDraggingScrollbar = false;
    private double scrollbarDragStartPositionY = 0;
    private double scrollbarDragStartScrollY = 0;

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText) {
        this(textRenderer, x, y, width, height, initialText, Integer.MAX_VALUE, true);
    }

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, int maxLines) {
        this(textRenderer, x, y, width, height, initialText, maxLines, true);
    }

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, int maxLines, boolean scrollingEnabled) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxLines = maxLines;
        this.scrollingEnabled = scrollingEnabled; // Set the flag
        setText(initialText);
    }

    public void setText(String text) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(Objects.requireNonNullElse(text, "").split("\n")));
        if (this.lines.isEmpty()) {
            this.lines.add("");
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

    public void insertText(String textToInsert) {
        String[] newLines = textToInsert.split("\n", -1);
        String currentLine = this.lines.get(cursorY);
        String beforeCursor = currentLine.substring(0, cursorX);
        String afterCursor = currentLine.substring(cursorX);

        this.lines.set(cursorY, beforeCursor + newLines[0]);

        if (newLines.length > 1) { // Multi-line paste
            // All middle lines are inserted as-is
            for (int i = 1; i < newLines.length - 1; i++) {
                if (this.lines.size() >= this.maxLines) break;
                this.lines.add(cursorY + i, newLines[i]);
            }
            // The last line of the paste gets the original afterCursor text appended
            if (this.lines.size() < this.maxLines) {
                int lastLineIndex = cursorY + newLines.length - 1;
                this.lines.add(lastLineIndex, newLines[newLines.length - 1] + afterCursor);
                setCursor(newLines[newLines.length - 1].length(), lastLineIndex);
            }
        } else { // Single-line insertion
            this.lines.set(cursorY, this.lines.get(cursorY) + afterCursor);
            setCursor(cursorX + textToInsert.length(), cursorY);
        }
        ensureCursorVisible();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.cursorBlinkTimer += delta;
        if (this.cursorBlinkTimer > 1f) {
            this.cursorBlinkTimer = 0;
        }

        int contentX = this.x + 5;
        int contentY = this.y + 5;
        int contentHeight = this.height - 10;

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
            if (lineYPos > this.y - textRenderer.fontHeight && lineYPos < this.y + this.height) {
                this.textRenderer.draw(matrices, this.lines.get(i), contentX, lineYPos, 0xFFFFFF);
            }
        }

        // --- Static underscore cursor logic ---
        if (this.focused && this.cursorBlinkTimer < 0.5f) {
            String line = this.lines.get(this.cursorY);
            int cursorRenderX = contentX + this.textRenderer.getWidth(line.substring(0, this.cursorX));
            int cursorRenderY = contentY + (cursorY * textRenderer.fontHeight) - (int) scrollY;
            fill(matrices, cursorRenderX, cursorRenderY - 1, cursorRenderX + 1, cursorRenderY + this.textRenderer.fontHeight, 0xFFFFFFFF);
        }

        com.mojang.blaze3d.systems.RenderSystem.disableScissor();

        if (this.scrollingEnabled && isScrollbarNeeded()) {
            renderScrollbar(matrices);
        }
    }

    private void renderScrollbar(MatrixStack matrices) {
        int scrollbarX = this.x + this.width - SCROLLBAR_WIDTH - 2;
        int trackHeight = this.height;
        int maxScroll = getMaxScroll();
        float contentHeight = lines.size() * textRenderer.fontHeight;

        // Calculate thumb size and position
        float thumbHeight = Math.max(10, (trackHeight / contentHeight) * trackHeight);
        float thumbY = (float) ((scrollY / maxScroll) * (trackHeight - thumbHeight));

        int thumbColor = isDraggingScrollbar ? 0xFFFFFFFF : 0x88FFFFFF;

        // Draw thumb
        fill(matrices, scrollbarX, this.y + (int) thumbY, scrollbarX + SCROLLBAR_WIDTH, this.y + (int) (thumbY + thumbHeight), thumbColor);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(isMouseOver(mouseX,mouseY)) {
            // --- Check for scrollbar click ---
            if (this.scrollingEnabled && isScrollbarNeeded() && mouseX >= this.x + this.width - SCROLLBAR_WIDTH - 2) {
                this.isDraggingScrollbar = true;
                this.scrollbarDragStartPositionY = mouseY;
                this.scrollbarDragStartScrollY = this.scrollY;
                this.focused = true;
                return true;
            }

            // --- Normal text area click ---
            this.focused = true;
            int lineIndex = (int) ((mouseY - (this.y + 5) + scrollY) / textRenderer.fontHeight);
            lineIndex = Math.max(0, Math.min(lineIndex, this.lines.size() - 1));

            String line = this.lines.get(lineIndex);
            int charIndex = this.textRenderer.trimToWidth(line, (int) (mouseX - (this.x + 5) + scrollX)).length();

            setCursor(charIndex, lineIndex);

            return true;
        }
        this.focused = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return Element.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrollingEnabled && isDraggingScrollbar) {
            double dragDelta = mouseY - this.scrollbarDragStartPositionY;
            float contentToTrackRatio = getMaxScroll() / (float)(this.height - Math.max(10, (this.height / (float)(lines.size() * textRenderer.fontHeight)) * this.height));

            this.scrollY = this.scrollbarDragStartScrollY + (dragDelta * contentToTrackRatio);
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.scrollingEnabled && isMouseOver(mouseX, mouseY)) {
            this.scrollY -= amount * 10;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) return false;

        // --- Handle word-wise cursor movement ---
        if (Screen.hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveCursorWordBack();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveCursorWordForward();
                return true;
            }
        }

        if (Screen.isSelectAll(keyCode)) { /* TODO: Implement selection */ return true; }
        if (Screen.isCopy(keyCode)) { /* TODO: Implement selection */ return true; }
        if (Screen.isPaste(keyCode)) { insertText(MinecraftClient.getInstance().keyboard.getClipboard()); return true; }
        if (Screen.isCut(keyCode)) { /* TODO: Implement selection */ return true; }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (this.lines.size() < this.maxLines) { // Check before adding a new line
                    String currentLine = this.lines.get(this.cursorY);
                    String beforeCursor = currentLine.substring(0, this.cursorX);
                    String afterCursor = currentLine.substring(this.cursorX);
                    this.lines.set(this.cursorY, beforeCursor);
                    this.lines.add(this.cursorY + 1, afterCursor);
                    setCursor(0, this.cursorY + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
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
            }
            case GLFW.GLFW_KEY_DELETE -> {
                String line = this.lines.get(this.cursorY);
                if (cursorX == line.length() && cursorY < this.lines.size() - 1) {
                    // Delete at the end of a line
                    String nextLine = this.lines.remove(cursorY + 1);
                    this.lines.set(cursorY, line + nextLine);
                } else if (cursorX < line.length()) {
                    // Delete in the middle of a line
                    String before = line.substring(0, cursorX);
                    String after = line.substring(cursorX + 1);
                    this.lines.set(this.cursorY, before + after);
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> { setCursor(this.cursorX, this.cursorY - 1); return true; }
            case GLFW.GLFW_KEY_DOWN -> { setCursor(this.cursorX, this.cursorY + 1); return true; }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorX == 0 && cursorY > 0) {
                    setCursor(this.lines.get(cursorY - 1).length(), cursorY - 1);
                } else {
                    setCursor(this.cursorX - 1, this.cursorY);
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorX == this.lines.get(cursorY).length() && cursorY < this.lines.size() - 1) {
                    setCursor(0, cursorY + 1);
                } else {
                    setCursor(this.cursorX + 1, this.cursorY);
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> { setCursor(0, this.cursorY); return true; }
            case GLFW.GLFW_KEY_END -> { setCursor(this.lines.get(this.cursorY).length(), this.cursorY); return true; }
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
        int contentHeight = this.height - 10;
        int topVisibleLine = (int) (scrollY / textRenderer.fontHeight);
        if (cursorY < topVisibleLine) {
            scrollY = cursorY * textRenderer.fontHeight;
        }

        int linesOnScreen = contentHeight / textRenderer.fontHeight;
        int bottomVisibleLine = topVisibleLine + linesOnScreen - 1;
        if (cursorY >= bottomVisibleLine + 1) {
            scrollY = (cursorY - linesOnScreen + 1) * textRenderer.fontHeight;
        }
        clampScroll();
    }

    private int getMaxScroll() {
        return Math.max(0, (this.lines.size() * textRenderer.fontHeight) - (height - 10));
    }
    private boolean isScrollbarNeeded() { return (this.lines.size() * textRenderer.fontHeight) > (height - 10); }

    private void clampScroll() {
        if (this.scrollY > getMaxScroll()) {
            this.scrollY = getMaxScroll();
        }
        if (this.scrollY < 0) {
            this.scrollY = 0;
        }
    }

    private void moveCursorWordBack() {
        if (cursorX == 0 && cursorY > 0) {
            setCursor(this.lines.get(cursorY - 1).length(), cursorY - 1);
            return;
        }
        String line = this.lines.get(cursorY);
        int pos = cursorX;
        while (pos > 0 && Character.isWhitespace(line.charAt(pos - 1))) {
            pos--;
        }
        while (pos > 0 && !Character.isWhitespace(line.charAt(pos - 1))) {
            pos--;
        }
        setCursor(pos, cursorY);
    }

    private void moveCursorWordForward() {
        String line = this.lines.get(cursorY);
        if (cursorX == line.length() && cursorY < lines.size() - 1) {
            setCursor(0, cursorY + 1);
            return;
        }

        int pos = cursorX;
        while (pos < line.length() && !Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
        while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
        setCursor(pos, cursorY);
    }


    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    @Override
    public SelectionType getType() { return this.focused ? SelectionType.FOCUSED : SelectionType.NONE; }
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) { /* Not needed */ }

}