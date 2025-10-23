package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.data.undoredo.TextAction;
import net.atif.buildnotes.data.undoredo.UndoManager;

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
    private boolean allowVerticalScroll;
    private boolean allowHorizontalScroll;


    private List<String> lines = Lists.newArrayList("");
    private int cursorX = 0; // column
    private int cursorY = 0; // line index
    private boolean focused = false;
    private double scrollY = 0;
    private double scrollX = 0;

    private static final int SCROLLBAR_THICKNESS = 6;
    private boolean isDraggingVScrollbar = false;
    private double vScrollbarDragStartY = 0;
    private double vScrollbarDragStartScrollY = 0;

    private boolean isDraggingHScrollbar = false;
    private double hScrollbarDragStartX = 0;
    private double hScrollbarDragStartScrollX = 0;

    // Selection as absolute indices across the joined text (includes '\n' between lines)
    private int selectionStart = 0;
    private int selectionEnd = 0;
    private int selectionAnchor = 0; // anchor for mouse dragging (fixed until mouse released)
    private long lastClickTime = 0;
    private int lastClickIndex = -1;
    private int clickCount = 0;
    private static final long DOUBLE_CLICK_INTERVAL_MS = 300;

    // dragging selection by mouse
    private boolean isDraggingText = false;

    private final UndoManager undoManager = new UndoManager(this); // NEW FIELD
    private String placeholderText;
    // caret blink
    private boolean caretVisible = true;
    private long lastBlinkTime = System.currentTimeMillis();
    private static final long BLINK_INTERVAL_MS = 500;

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText) {
        this(textRenderer, x, y, width, height, initialText,"", Integer.MAX_VALUE, true);
    }

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, int maxLines) {
        this(textRenderer, x, y, width, height, initialText,"", maxLines, true);
    }

    public MultiLineTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String initialText, String placeholder, int maxLines, boolean scrollingEnabled) {
        this.textRenderer = textRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxLines = maxLines;
        this.scrollingEnabled = scrollingEnabled;
        this.placeholderText = placeholder;

        boolean defaultVerticalScroll = this.scrollingEnabled;
        if (maxLines == 1) {
            defaultVerticalScroll = false;
        }
        this.allowVerticalScroll = defaultVerticalScroll;
        this.allowHorizontalScroll = true;

        setText(initialText);
    }

    public void setText(String text) {
        this.lines.clear();
        this.lines.addAll(Arrays.asList(Objects.requireNonNullElse(text, "").split("\n", -1)));
        if (this.lines.isEmpty()) this.lines.add("");
        this.setCursorToEnd();
        this.clearSelection();
        this.focused = false;
        this.scrollX = 0;
        this.scrollY = 0;

        clampScroll();
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

    // ---------- Absolute index helpers ----------
    private int getTotalLength() {
        int total = 0;
        for (String s : lines) total += s.length();
        total += Math.max(0, lines.size() - 1); // newlines
        return total;
    }

    private int getAbsoluteIndex(int lineIndex, int col) {
        int abs = 0;
        for (int i = 0; i < lineIndex; i++) {
            abs += lines.get(i).length() + 1; // line + newline
        }
        abs += Math.max(0, Math.min(col, lines.get(lineIndex).length()));
        return abs;
    }

    private int[] getLineColFromAbsolute(int absoluteIndex) {
        int remaining = Math.max(0, Math.min(absoluteIndex, getTotalLength()));
        for (int i = 0; i < lines.size(); i++) {
            int lineLen = lines.get(i).length();
            if (remaining <= lineLen) {
                return new int[]{i, remaining};
            }
            remaining -= (lineLen + 1); // consume line + newline
            if (remaining < 0) {
                return new int[]{i, lineLen};
            }
        }
        int last = lines.size() - 1;
        return new int[]{last, lines.get(last).length()};
    }

    public int getCursorAbsolute() {
        return getAbsoluteIndex(cursorY, cursorX);
    }

    public int getSelectionStartAbsolute() {
        return this.selectionStart;
    }

    public int getSelectionEndAbsolute() {
        return this.selectionEnd;
    }

    public void setCursorFromAbsolute(int absoluteIndex) {
        int[] lc = getLineColFromAbsolute(absoluteIndex);
        // lc[0] = line, lc[1] = col
        setCursor(lc[1], lc[0]);
    }

    // ---------- Selection helpers ----------
    public void setSelectionAbsolute(int a, int b) {
        int t = Math.max(0, Math.min(a, getTotalLength()));
        int e = Math.max(0, Math.min(b, getTotalLength()));
        if (t <= e) {
            selectionStart = t;
            selectionEnd = e;
        } else {
            selectionStart = e;
            selectionEnd = t;
        }
    }

    private void clearSelection() {
        int abs = getAbsoluteIndex(cursorY, cursorX);
        selectionStart = abs;
        selectionEnd = abs;
        selectionAnchor = abs;
    }

    private boolean hasSelection() {
        return selectionEnd > selectionStart;
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        StringBuilder sb = new StringBuilder();
        int start = selectionStart;
        int end = selectionEnd;
        int[] sLC = getLineColFromAbsolute(start);
        int[] eLC = getLineColFromAbsolute(end);
        if (sLC[0] == eLC[0]) {
            return lines.get(sLC[0]).substring(sLC[1], eLC[1]);
        }
        sb.append(lines.get(sLC[0]).substring(sLC[1]));
        sb.append('\n');
        for (int i = sLC[0] + 1; i < eLC[0]; i++) {
            sb.append(lines.get(i));
            sb.append('\n');
        }
        sb.append(lines.get(eLC[0]).substring(0, eLC[1]));
        return sb.toString();
    }

    private void deleteSelection() {
        if (!hasSelection()) return;

        final int start = selectionStart;
        final int end = selectionEnd;
        final String selectedText = getSelectedText();

        // Create an action that knows how to delete AND re-insert the text
        TextAction action = new TextAction() {
            @Override
            public void execute() {
                _deleteTextInternal(start, end);
            }
            @Override
            public void undo() {
                _insertTextInternal(start, selectedText);
            }
        };

        undoManager.perform(action);
    }

    // In MultiLineTextFieldWidget.java

    private void selectWordAt(int absoluteIndex) {
        int[] lc = getLineColFromAbsolute(absoluteIndex);
        int line = lc[0];
        int col = lc[1];
        String lineStr = this.lines.get(line);

        if (lineStr.isEmpty()) return; // Nothing to select on an empty line

        // Find the start of the word by moving backward
        int wordStartCol = col;
        // If the cursor is at the end of a word, move it back one to be "inside" it
        if (wordStartCol > 0 && wordStartCol >= lineStr.length() || Character.isWhitespace(lineStr.charAt(wordStartCol))) {
            if (wordStartCol > 0) wordStartCol--;
        }
        while (wordStartCol > 0 && !Character.isWhitespace(lineStr.charAt(wordStartCol - 1))) {
            wordStartCol--;
        }

        // Find the end of the word by moving forward
        int wordEndCol = col;
        while (wordEndCol < lineStr.length() && !Character.isWhitespace(lineStr.charAt(wordEndCol))) {
            wordEndCol++;
        }

        int selectionStartAbs = getAbsoluteIndex(line, wordStartCol);
        int selectionEndAbs = getAbsoluteIndex(line, wordEndCol);

        setSelectionAbsolute(selectionStartAbs, selectionEndAbs);
        setCursorFromAbsolute(selectionEndAbs); // Move cursor to the end of the new selection
    }

    private void selectLineAt(int absoluteIndex) {
        int[] lc = getLineColFromAbsolute(absoluteIndex);
        int line = lc[0];

        int lineStartAbs = getAbsoluteIndex(line, 0);
        int lineEndAbs = getAbsoluteIndex(line, this.lines.get(line).length());

        setSelectionAbsolute(lineStartAbs, lineEndAbs);
        setCursorFromAbsolute(lineEndAbs); // Move cursor to the end of the line
    }

    // ---------- Insertion ----------
    public void insertText(String textToInsert) {
        if (textToInsert == null || textToInsert.isEmpty()) return;
        if (hasSelection()) deleteSelection();

        final int start = getCursorAbsolute();
        final String text = textToInsert;

        TextAction action = new TextAction() {
            @Override
            public void execute() {
                _insertTextInternal(start, text);
            }
            @Override
            public void undo() {
                _deleteTextInternal(start, start + text.length());
            }
        };
        undoManager.perform(action);
    }

    // ---------- Rendering ----------
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int padding = 5;
        int contentX = this.x + padding;
        int contentY = this.y + padding;
        int contentWidth = this.width - padding * 2;
        int contentHeight = this.height - padding * 2;

        // Reserve space for scrollbars if needed
        boolean vNeeded = isScrollbarNeededV();
        boolean hNeeded = isScrollbarNeededH();
        if (vNeeded) contentWidth -= (SCROLLBAR_THICKNESS + 2);
        if (hNeeded) contentHeight -= (SCROLLBAR_THICKNESS + 2);

        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int scissorX = (int) (this.x * scale);
        int scissorY = (int) (MinecraftClient.getInstance().getWindow().getFramebufferHeight() - ((this.y + this.height) * scale));
        int scissorWidth = (int) (this.width * scale);
        int scissorHeight = (int) (this.height * scale);
        com.mojang.blaze3d.systems.RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        if (getText().isEmpty() && !this.focused && this.placeholderText != null && !this.placeholderText.isEmpty()) {
            // Draw placeholder text, respecting horizontal scroll
            int drawX = contentX - (int) Math.round(scrollX);
            textRenderer.draw(matrices, this.placeholderText, drawX, contentY, 0xFF808080); // Gray color
        }

        int firstVisibleLine = (int) (scrollY / textRenderer.fontHeight);
        int lastVisibleLine = Math.min(this.lines.size() - 1, firstVisibleLine + (contentHeight / textRenderer.fontHeight) + 1);

        // Draw selection background (per-line)
        if (hasSelection()) {
            int selStart = selectionStart;
            int selEnd = selectionEnd;
            for (int i = firstVisibleLine; i <= lastVisibleLine; i++) {
                int lineStartAbs = getAbsoluteIndex(i, 0);
                int lineEndAbs = lineStartAbs + lines.get(i).length();
                int interStart = Math.max(selStart, lineStartAbs);
                int interEnd = Math.min(selEnd, lineEndAbs);
                if (interStart < interEnd) {
                    int startCol = interStart - lineStartAbs;
                    int endCol = interEnd - lineStartAbs;
                    int sx = contentX + (int) Math.round(textRenderer.getWidth(lines.get(i).substring(0, startCol)) - scrollX);
                    int ex = contentX + (int) Math.round(textRenderer.getWidth(lines.get(i).substring(0, endCol)) - scrollX);
                    int lineYPos = contentY + (i * textRenderer.fontHeight) - (int) scrollY;
                    fill(matrices, sx, lineYPos, ex, lineYPos + textRenderer.fontHeight, 0x8855AADD);
                }
            }
        }

        // Draw lines (with horizontal scroll applied)
        for (int i = firstVisibleLine; i <= lastVisibleLine; i++) {
            int lineYPos = contentY + (i * textRenderer.fontHeight) - (int) scrollY;
            if (lineYPos > this.y - textRenderer.fontHeight && lineYPos < this.y + this.height) {
                int drawX = contentX - (int) Math.round(scrollX);
                this.textRenderer.draw(matrices, this.lines.get(i), drawX, lineYPos, 0xFFFFFF);
            }
        }

        // caret blink
        long now = System.currentTimeMillis();
        if (now - lastBlinkTime >= BLINK_INTERVAL_MS) {
            caretVisible = !caretVisible;
            lastBlinkTime = now;
        }

        // Caret drawing (vertical bar) - make it a bit wider and taller for visibility
        if (this.focused && caretVisible) {
            int paddingTop = 1;
            int paddingBottom = 1;
            if (cursorY >= firstVisibleLine && cursorY <= lastVisibleLine) {
                String line = this.lines.get(this.cursorY);
                int caretPixelX = contentX + (int) Math.round(textRenderer.getWidth(line.substring(0, this.cursorX)) - scrollX);
                int caretYPos = contentY + (cursorY * textRenderer.fontHeight) - (int) scrollY;
                int top = caretYPos - paddingTop;
                int bottom = caretYPos + textRenderer.fontHeight + paddingBottom;
                // draw 2px wide vertical caret centered at caretPixelX
                fill(matrices, caretPixelX, top, caretPixelX + 1, bottom, 0xFFFFFFFF);
            }
        }

        com.mojang.blaze3d.systems.RenderSystem.disableScissor();

        // Draw scrollbars
        if (this.scrollingEnabled && vNeeded) renderVScrollbar(matrices, contentX, contentY, contentHeight);
        if (this.scrollingEnabled && hNeeded) renderHScrollbar(matrices, contentX, contentY, contentWidth, contentHeight);
    }

    private void renderVScrollbar(MatrixStack matrices, int contentX, int contentY, int contentHeight) {
        int scrollbarX = this.x + this.width - SCROLLBAR_THICKNESS - 2;
        int trackHeight = contentHeight;
        int maxScroll = getMaxScrollV();
        float contentPixelHeight = lines.size() * textRenderer.fontHeight;
        float thumbHeight = Math.max(10, (trackHeight / contentPixelHeight) * trackHeight);
        float thumbY = (float) ((scrollY / (double) Math.max(1, maxScroll)) * (trackHeight - thumbHeight));
        int thumbColor = isDraggingVScrollbar ? 0xFFFFFFFF : 0x88FFFFFF;
        fill(matrices, scrollbarX, this.y + 5 + (int) thumbY, scrollbarX + SCROLLBAR_THICKNESS, this.y + 5 + (int) (thumbY + thumbHeight), thumbColor);
    }

    private void renderHScrollbar(MatrixStack matrices, int contentX, int contentY, int contentWidth, int contentHeight) {
        int scrollbarY = this.y + this.height - SCROLLBAR_THICKNESS - 2;
        // compute max horizontal content width
        int maxLinePixel = getMaxLinePixelWidth();
        int visible = contentWidth;
        if (maxLinePixel <= 0) return;
        float thumbWidth = Math.max(10, (visible / (float) maxLinePixel) * visible);
        float thumbX = (float) ((scrollX / (double) Math.max(1, getMaxScrollH())) * (visible - thumbWidth));
        int thumbColor = isDraggingHScrollbar ? 0xFFFFFFFF : 0x88FFFFFF;
        fill(matrices, contentX + (int) thumbX, scrollbarY, contentX + (int) (thumbX + thumbWidth), scrollbarY + SCROLLBAR_THICKNESS, thumbColor);
    }

    // ---------- Mouse handling ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            // vertical scrollbar click?
            if (this.scrollingEnabled && isScrollbarNeededV()) {
                int vXStart = this.x + this.width - SCROLLBAR_THICKNESS - 2;
                if (mouseX >= vXStart) {
                    this.isDraggingVScrollbar = true;
                    this.vScrollbarDragStartY = mouseY;
                    this.vScrollbarDragStartScrollY = this.scrollY;
                    this.focused = true;
                    return true;
                }
            }
            // horizontal scrollbar click?
            if (this.scrollingEnabled && isScrollbarNeededH()) {
                int padding = 5;
                int contentX = this.x + padding;
                int contentY = this.y + padding;
                int contentWidth = this.width - padding * 2 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0);
                int scrollbarY = this.y + this.height - SCROLLBAR_THICKNESS - 2;
                if (mouseY >= scrollbarY) {
                    // click on horizontal thumb region
                    this.isDraggingHScrollbar = true;
                    this.hScrollbarDragStartX = mouseX;
                    this.hScrollbarDragStartScrollX = this.scrollX;
                    this.focused = true;
                    return true;
                }
            }

            // normal text area click
            this.focused = true;
            int clickedAbs = absoluteIndexFromMouse(mouseX, mouseY);
            long now = System.currentTimeMillis();

            // --- NEW: Double/Triple click detection logic ---
            if (now - lastClickTime < DOUBLE_CLICK_INTERVAL_MS && clickedAbs == lastClickIndex) {
                clickCount++;
            } else {
                clickCount = 1;
            }

            // Update state for the next click
            this.lastClickTime = now;
            this.lastClickIndex = clickedAbs;

            // Handle actions based on click count
            if (clickCount == 1) { // SINGLE CLICK
                boolean shift = Screen.hasShiftDown();
                if (shift) {
                    setSelectionAbsolute(selectionAnchor, clickedAbs);
                    setCursorFromAbsolute(clickedAbs);
                } else {
                    selectionAnchor = clickedAbs;
                    setSelectionAbsolute(clickedAbs, clickedAbs);
                    setCursorFromAbsolute(clickedAbs);
                    this.isDraggingText = true; // Only start dragging on single click
                }
            } else if (clickCount == 2) { // DOUBLE CLICK
                selectWordAt(clickedAbs);
                this.isDraggingText = false;
            } else if (clickCount == 3) { // TRIPLE CLICK
                selectLineAt(clickedAbs);
                this.isDraggingText = false;
                clickCount = 0; // Reset after triple-click to restart the cycle
            }

            return true;
        }
        this.focused = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingVScrollbar = false;
        isDraggingHScrollbar = false;
        isDraggingText = false;
        return Element.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.scrollingEnabled && isDraggingVScrollbar) {
            double dragDelta = mouseY - this.vScrollbarDragStartY;
            int trackHeight = this.height - 10 - (isScrollbarNeededH() ? (SCROLLBAR_THICKNESS + 2) : 0);
            double maxScroll = Math.max(1, getMaxScrollV());
            double contentPixelHeight = lines.size() * textRenderer.fontHeight;
            double thumbHeight = Math.max(10, (trackHeight / contentPixelHeight) * trackHeight);
            double toTrack = (trackHeight - thumbHeight);
            if (toTrack <= 0) return true;
            this.scrollY = this.vScrollbarDragStartScrollY + (dragDelta * (maxScroll / toTrack));
            clampScroll();
            return true;
        }
        if (this.scrollingEnabled && isDraggingHScrollbar) {
            double dragDelta = mouseX - this.hScrollbarDragStartX;
            int padding = 5;
            int contentWidth = this.width - padding * 2 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0);
            int maxH = getMaxScrollH();
            double thumbWidth = Math.max(10, (contentWidth / (float) Math.max(1, getMaxLinePixelWidth())) * contentWidth);
            double toTrack = (contentWidth - thumbWidth);
            if (toTrack <= 0) return true;
            this.scrollX = this.hScrollbarDragStartScrollX + (dragDelta * (maxH / toTrack));
            clampScroll();
            return true;
        }
        if (isDraggingText) {
            int abs = absoluteIndexFromMouse(mouseX, mouseY);
            // anchor remains as when mouseClicked started
            setSelectionAbsolute(selectionAnchor, abs);
            setCursorFromAbsolute(abs);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // If shift is down -> horizontal scroll, otherwise vertical
        if (!isMouseOver(mouseX, mouseY)) return false;

        boolean shift = Screen.hasShiftDown();

        if (shift && this.allowHorizontalScroll) {
            this.scrollX -= amount * 10;
            clampScroll();
            return true;
        } else if (!shift && this.allowVerticalScroll) {
            this.scrollY -= amount * 10;
            clampScroll();
            return true;
        }

        return false;
    }

    private int absoluteIndexFromMouse(double mouseX, double mouseY) {
        int padding = 5;
        int contentX = this.x + padding;
        int contentY = this.y + padding;
        int contentWidth = this.width - padding * 2 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0);
        int clickedLine = (int) ((mouseY - (this.y + padding) + scrollY) / textRenderer.fontHeight);
        clickedLine = Math.max(0, Math.min(clickedLine, this.lines.size() - 1));
        int relX = (int) Math.round(mouseX - (contentX) + scrollX);
        if (relX < 0) relX = 0;
        String line = this.lines.get(clickedLine);
        int charIndex = this.textRenderer.trimToWidth(line, relX).length();
        return getAbsoluteIndex(clickedLine, charIndex);
    }

    // ---------- Keyboard ----------
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused) return false;

        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();

        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) {
            undoManager.undo();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) {
            undoManager.redo();
            return true;
        }

        // ctrl+word moves (absolute space)
        if (ctrl && keyCode == GLFW.GLFW_KEY_LEFT) {
            int oldAbs = getAbsoluteIndex(cursorY, cursorX);
            int newAbs = moveWordBackAbsolute(oldAbs);
            moveCursorToAbsolute(newAbs, shift);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_RIGHT) {
            int oldAbs = getAbsoluteIndex(cursorY, cursorX);
            int newAbs = moveWordForwardAbsolute(oldAbs);
            moveCursorToAbsolute(newAbs, shift);
            return true;
        }

        if (Screen.isSelectAll(keyCode)) {
            setSelectionAbsolute(0, getTotalLength());
            setCursorFromAbsolute(getTotalLength());
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            if (hasSelection()) MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null && !clip.isEmpty()) insertText(clip);
            return true;
        }
        if (Screen.isCut(keyCode)) {
            if (hasSelection()) {
                MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
                deleteSelection();
            }
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (this.lines.size() < this.maxLines) {
                    insertText("\n");
                }
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                    return true;
                }
                if (ctrl) {
                    int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                    if (oldAbs > 0) {
                        int newAbs = moveWordBackAbsolute(oldAbs);
                        setSelectionAbsolute(newAbs, oldAbs);
                        deleteSelection();
                    }
                    return true;
                }
                if (cursorX == 0 && cursorY > 0) {
                    String lineToMerge = this.lines.remove(this.cursorY);
                    int prevLineIndex = this.cursorY - 1;
                    String prevLine = this.lines.get(prevLineIndex);
                    int newCursorX = prevLine.length();
                    this.lines.set(prevLineIndex, prevLine + lineToMerge);
                    setCursor(newCursorX, prevLineIndex);
                } else if (cursorX > 0) {
                    String line = this.lines.get(this.cursorY);
                    String before = line.substring(0, cursorX - 1);
                    String after = line.substring(cursorX);
                    this.lines.set(this.cursorY, before + after);
                    setCursor(cursorX - 1, cursorY);
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                    return true;
                }
                if (ctrl) {
                    int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                    if (oldAbs < getTotalLength()) {
                        int newAbs = moveWordForwardAbsolute(oldAbs);
                        setSelectionAbsolute(oldAbs, newAbs);
                        deleteSelection();
                    }
                    return true;
                }
                String line = this.lines.get(this.cursorY);
                if (cursorX == line.length() && cursorY < this.lines.size() - 1) {
                    String nextLine = this.lines.remove(cursorY + 1);
                    this.lines.set(cursorY, line + nextLine);
                } else if (cursorX < line.length()) {
                    String before = line.substring(0, cursorX);
                    String after = line.substring(cursorX + 1);
                    this.lines.set(this.cursorY, before + after);
                }
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                int newLine = Math.max(0, cursorY - 1);
                int newCol = Math.min(cursorX, lines.get(newLine).length());
                int newAbs = getAbsoluteIndex(newLine, newCol);
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                int newLine = Math.min(lines.size() - 1, cursorY + 1);
                int newCol = Math.min(cursorX, lines.get(newLine).length());
                int newAbs = getAbsoluteIndex(newLine, newCol);
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                if (oldAbs == 0) return true;
                int newAbs = oldAbs - 1;
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                if (oldAbs >= getTotalLength()) return true;
                int newAbs = oldAbs + 1;
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                int newAbs = getAbsoluteIndex(cursorY, 0);
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                int oldAbs = getAbsoluteIndex(cursorY, cursorX);
                int newAbs = getAbsoluteIndex(cursorY, lines.get(cursorY).length());
                moveCursorToAbsolute(newAbs, shift);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.focused) {
            if (chr == 0 || Character.isISOControl(chr)) return false;
            insertText(Character.toString(chr));
            return true;
        }
        return false;
    }

    private void ensureCursorVisible() {
        int padding = 5;
        int contentX = this.x + padding;
        int contentY = this.y + padding;
        int contentWidth = this.width - padding * 2 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0);
        int contentHeight = this.height - padding * 2 - (isScrollbarNeededH() ? (SCROLLBAR_THICKNESS + 2) : 0);

        // vertical
        int topVisibleLine = (int) (scrollY / textRenderer.fontHeight);
        if (cursorY < topVisibleLine) scrollY = cursorY * textRenderer.fontHeight;
        int linesOnScreen = contentHeight / textRenderer.fontHeight;
        if (cursorY >= topVisibleLine + linesOnScreen) scrollY = (cursorY - linesOnScreen + 1) * textRenderer.fontHeight;

        // horizontal - ensure the caret's pixel location (within the line) is visible
        String line = lines.get(cursorY);
        int caretPixel = (int) Math.round(textRenderer.getWidth(line.substring(0, cursorX)));
        if (caretPixel - scrollX < 0) {
            scrollX = caretPixel;
        } else if (caretPixel - scrollX > contentWidth - 4) {
            scrollX = caretPixel - (contentWidth - 4);
        }
        clampScroll();
    }

    // ---------- Scroll metrics ----------
    private int getMaxScrollV() {
        return Math.max(0, (this.lines.size() * textRenderer.fontHeight) - (height - 10 - (isScrollbarNeededH() ? (SCROLLBAR_THICKNESS + 2) : 0)));
    }

    private boolean isScrollbarNeededV() {
        return (this.lines.size() * textRenderer.fontHeight) > (height - 10);
    }

    private int getMaxLinePixelWidth() {
        int max = 0;
        for (String s : lines) {
            int w = textRenderer.getWidth(s);
            if (w > max) max = w;
        }
        return max;
    }

    private int getMaxScrollH() {
        int padding = 5;
        int contentWidth = this.width - padding * 2 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0);
        int maxLine = getMaxLinePixelWidth();
        return Math.max(0, maxLine - contentWidth);
    }

    private boolean isScrollbarNeededH() {
        return getMaxLinePixelWidth() > (this.width - 10 - (isScrollbarNeededV() ? (SCROLLBAR_THICKNESS + 2) : 0));
    }

    private void clampScroll() {
        if (allowVerticalScroll) {
            double maxV = getMaxScrollV();
            if (this.scrollY > maxV) this.scrollY = maxV;
            if (this.scrollY < 0) this.scrollY = 0;
        } else {
            this.scrollY = 0;
        }

        if (allowHorizontalScroll) {
            double maxH = getMaxScrollH();
            if (this.scrollX > maxH) this.scrollX = maxH;
            if (this.scrollX < 0) this.scrollX = 0;
        } else {
            this.scrollX = 0;
        }
    }

    // ---------- Cursor & selection movement ----------
    // move cursor absolute with correct selection anchor behavior
    private void moveCursorToAbsolute(int newAbs, boolean keepSelection) {
        newAbs = Math.max(0, Math.min(newAbs, getTotalLength()));
        int oldAbs = getAbsoluteIndex(cursorY, cursorX);
        if (keepSelection) {
            if (!hasSelection()) {
                // start selection anchored at old position
                selectionAnchor = oldAbs;
                setSelectionAbsolute(selectionAnchor, newAbs);
            } else {
                // extend/shrink selection anchored at selectionAnchor
                setSelectionAbsolute(selectionAnchor, newAbs);
            }
        } else {
            setCursorFromAbsolute(newAbs);
            clearSelection();
            return;
        }
        setCursorFromAbsolute(newAbs);
        ensureCursorVisible();
    }

    private int moveWordBackAbsolute(int abs) {
        if (abs <= 0) return 0;
        int[] lc = getLineColFromAbsolute(abs);
        int line = lc[0], col = lc[1];
        if (col == 0) {
            if (line == 0) return 0;
            int prevLine = line - 1;
            return getAbsoluteIndex(prevLine, lines.get(prevLine).length());
        }
        String lineStr = lines.get(line);
        int pos = col;
        while (pos > 0 && Character.isWhitespace(lineStr.charAt(pos - 1))) pos--;
        while (pos > 0 && !Character.isWhitespace(lineStr.charAt(pos - 1))) pos--;
        return getAbsoluteIndex(line, pos);
    }

    private int moveWordForwardAbsolute(int abs) {
        if (abs >= getTotalLength()) return getTotalLength();
        int[] lc = getLineColFromAbsolute(abs);
        int line = lc[0], col = lc[1];
        String lineStr = lines.get(line);
        if (col == lineStr.length()) {
            if (line >= lines.size() - 1) return getTotalLength();
            return getAbsoluteIndex(line + 1, 0);
        }
        int pos = col;
        while (pos < lineStr.length() && !Character.isWhitespace(lineStr.charAt(pos))) pos++;
        while (pos < lineStr.length() && Character.isWhitespace(lineStr.charAt(pos))) pos++;
        return getAbsoluteIndex(line, pos);
    }

    // ---------- Misc ----------
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public void _deleteTextInternal(int startAbsolute, int endAbsolute) {
        int[] sLC = getLineColFromAbsolute(startAbsolute);
        int[] eLC = getLineColFromAbsolute(endAbsolute);

        if (sLC[0] == eLC[0]) {
            String line = lines.get(sLC[0]);
            String before = line.substring(0, sLC[1]);
            String after = line.substring(eLC[1]);
            lines.set(sLC[0], before + after);
        } else {
            String firstPart = lines.get(sLC[0]).substring(0, sLC[1]);
            String lastPart = lines.get(eLC[0]).substring(eLC[1]);
            for (int i = eLC[0]; i > sLC[0]; i--) lines.remove(i);
            lines.set(sLC[0], firstPart + lastPart);
        }
        setCursorFromAbsolute(startAbsolute);
        clearSelection();
    }

    public void _insertTextInternal(int startAbsolute, String textToInsert) {
        setCursorFromAbsolute(startAbsolute); // Set cursor to know where to insert

        String[] parts = textToInsert.split("\n", -1);
        String currentLine = lines.get(cursorY);
        String beforeCursor = currentLine.substring(0, cursorX);
        String afterCursor = currentLine.substring(cursorX);

        if (parts.length == 1) {
            lines.set(cursorY, beforeCursor + parts[0] + afterCursor);
            setCursor(beforeCursor.length() + parts[0].length(), cursorY);
        } else {
            lines.set(cursorY, beforeCursor + parts[0]);
            int insertAt = cursorY + 1;
            for (int i = 1; i < parts.length - 1; i++) {
                if (lines.size() >= maxLines) break;
                lines.add(insertAt, parts[i]);
                insertAt++;
            }
            if (lines.size() < maxLines) {
                lines.add(insertAt, parts[parts.length - 1] + afterCursor);
                setCursor(parts[parts.length - 1].length(), insertAt);
            } else {
                String last = lines.get(lines.size() - 1);
                lines.set(lines.size() - 1, last + afterCursor);
                setCursor(lines.get(lines.size() - 1).length(), lines.size() - 1);
            }
        }
        ensureCursorVisible();
    }

    @Override
    public SelectionType getType() { return this.focused ? SelectionType.FOCUSED : SelectionType.NONE; }
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) { /* Not needed */ }
}
