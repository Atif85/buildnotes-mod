package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public abstract class AbstractListWidget<E extends AbstractListWidget.Entry<E>> extends ObjectSelectionList<E> {

    protected final MainScreen parentScreen;
    private boolean visible = false;
    private static final int FADE_HEIGHT = 12;

    // double-click tracking
    private long lastClickTime = 0L;
    private E lastClickedEntry = null;
    private static final long DOUBLE_CLICK_MS = 250L;

    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private double scrollbarDragStartMouseY;

    public AbstractListWidget(MainScreen parent, Minecraft client, int top, int bottom, int itemHeight) {
        // We pass the parent's width and height to the super constructor.
        super(client, parent.width, bottom - top, top, itemHeight);

        this.parentScreen = parent;
    }

    // --- SHARED VISIBILITY LOGIC ---
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!this.visible) return false;


        double mouseX = event.x();
        double mouseY = event.y();

        if (this.isMouseOver(mouseX, mouseY) && mouseX >= this.scrollBarX() && mouseX < this.scrollBarX() + SCROLLBAR_WIDTH) {
            this.isDraggingScrollbar = true;
            return true;
        }

        this.isDraggingScrollbar = false;
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.isDraggingScrollbar = false;
        super.onRelease(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (this.isDraggingScrollbar) {
            int trackHeight = this.getHeight();
            float maxScroll = this.maxScrollAmount();
            if (maxScroll <= 0) return true;

            int thumbHeight = this.scrollerHeight();
            float draggableHeight = (float)trackHeight - thumbHeight;
            if (draggableHeight <= 0) return true; // Cannot drag if thumb fills the track

            // Calculate the ratio of scrollable content to draggable area
            float ratio = maxScroll / draggableHeight;

            this.setScrollAmount(this.scrollAmount() + (offsetY * ratio));

            return true;
        }
        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        graphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());

        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
        renderCustomScrollbar(graphics);

        graphics.disableScissor();

        // Top fade overlay
        int left = this.getX();
        int right = this.getRight();

        int topY = this.getY();

        graphics.fillGradient(left, topY, right, topY + FADE_HEIGHT,
                Colors.FADE_GRADIENT_TOP, Colors.FADE_GRADIENT_BOTTOM);

        int bottomY = this.getBottom() - FADE_HEIGHT;
        graphics.fillGradient(left, bottomY, right, this.getBottom(),
                Colors.FADE_GRADIENT_BOTTOM, Colors.FADE_GRADIENT_TOP);

    }

    protected void renderCustomScrollbar(GuiGraphicsExtractor graphics) {
        int maxScroll = this.maxScrollAmount();
        if (maxScroll <= 0) return; // Don't render if not scrollable

        int scrollbarX = this.scrollBarX();
        int trackHeight = this.getHeight();

        int thumbHeight = this.scrollerHeight();

        float maxThumbY = trackHeight - thumbHeight;

        float thumbY = (float)this.scrollAmount() / (float)maxScroll * maxThumbY;

        thumbY = Math.min(thumbY, maxThumbY);

        int thumbColor = isDraggingScrollbar ? Colors.SCROLLBAR_THUMB_ACTIVE : Colors.SCROLLBAR_THUMB_INACTIVE;

        graphics.fill(scrollbarX, this.getY() + (int) thumbY, scrollbarX + SCROLLBAR_WIDTH, this.getY() + (int) (thumbY + thumbHeight), thumbColor);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {

    }

    // --- SHARED LAYOUT METHODS ---
    @Override
    public int getRowWidth() {
        // Make the list entries 3/5 of the screen's width.
        return (int) (this.parentScreen.width * 0.6);
    }

    @Override
    protected int scrollBarX() {
        int listWidth = getRowWidth();
        int xStart = (this.width - listWidth) / 2; // center the list
        return xStart + listWidth + 4; // small padding from edge
    }


    // --- SHARED UTILITY OVERRIDES ---
    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        // We don't need narrations for this UI.
    }

    protected void handleEntryClick(E entry) {
        long now = System.currentTimeMillis();
        if (entry == this.lastClickedEntry && (now - this.lastClickTime) <= DOUBLE_CLICK_MS) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );

            // double click detected
            if (this.parentScreen != null) {
                this.parentScreen.openSelected();
            }
            // reset to avoid triple click detection
            this.lastClickedEntry = null;
            this.lastClickTime = 0L;
            return;
        }
        // not a double click, register this as the last click
        this.lastClickedEntry = entry;
        this.lastClickTime = now;
    }

    public static abstract class Entry<E extends Entry<E>> extends ObjectSelectionList.Entry<E> {
    }

}