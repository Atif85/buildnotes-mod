package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.PositionedSoundInstance;

public abstract class AbstractListWidget<E extends AbstractListWidget.Entry<E>> extends EntryListWidget<E> {

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

    public AbstractListWidget(MainScreen parent, MinecraftClient client, int top, int bottom, int itemHeight) {
        // We pass the parent's width and height to the super constructor.
        super(client, parent.width, bottom - top, top, itemHeight);

        this.parentScreen = parent;
    }

    // --- SHARED VISIBILITY LOGIC ---
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        if (this.isMouseOver(mouseX, mouseY) && mouseX >= this.getScrollbarX() && mouseX < this.getScrollbarX() + SCROLLBAR_WIDTH) {
            this.isDraggingScrollbar = true;
            this.scrollbarDragStartMouseY = mouseY;
            return true;
        }
        this.isDraggingScrollbar = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDraggingScrollbar) {
            int trackHeight = this.getHeight();
            float maxScroll = this.getMaxScrollY();
            float thumbHeight = Math.max(10, (float)(trackHeight * trackHeight) / (float)this.getContentsHeightWithPadding());
            float draggableHeight = trackHeight - thumbHeight;

            if (draggableHeight <= 0) return true; // Cannot drag if thumb fills the track

            // Calculate the ratio of scrollable content to draggable area
            float ratio = maxScroll / draggableHeight;

            // Update scroll amount based on mouse movement since the drag started
            double relativeMouseY = mouseY - this.scrollbarDragStartMouseY;
            this.setScrollY(this.getScrollY() + (relativeMouseY * ratio));

            // Update the start position for the next frame's calculation
            this.scrollbarDragStartMouseY = mouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && super.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        context.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());

        super.renderWidget(context, mouseX, mouseY, delta);
        renderCustomScrollbar(context);

        context.disableScissor();

        // Top fade overlay
        int left = this.getX();
        int right = this.getRight();

        int topY = this.getY();

        context.fillGradient(left, topY, right, topY + FADE_HEIGHT,
                Colors.FADE_GRADIENT_TOP, Colors.FADE_GRADIENT_BOTTOM);

        int bottomY = this.getBottom() - FADE_HEIGHT;
        context.fillGradient(left, bottomY, right, this.getBottom(),
                Colors.FADE_GRADIENT_BOTTOM, Colors.FADE_GRADIENT_TOP);

    }

    @Override
    protected void drawMenuListBackground(DrawContext context) {
    }

    @Override
    protected void drawHeaderAndFooterSeparators(DrawContext context) {
    }

    protected void renderCustomScrollbar(DrawContext context) {
        int maxScroll = this.getMaxScrollY();
        if (maxScroll <= 0) return; // Don't render if not scrollable

        int scrollbarX = this.getScrollbarX();
        int trackHeight = this.getHeight();

        float thumbHeight = Math.max(10, (float)(trackHeight * trackHeight) / (float)this.getContentsHeightWithPadding());

        float maxThumbY = trackHeight - thumbHeight;

        float thumbY = (float)this.getScrollY() / (float)maxScroll * maxThumbY;

        thumbY = Math.min(thumbY, maxThumbY);

        int thumbColor = isDraggingScrollbar ? Colors.SCROLLBAR_THUMB_ACTIVE : Colors.SCROLLBAR_THUMB_INACTIVE;

        context.fill(scrollbarX, this.getY() + (int) thumbY, scrollbarX + SCROLLBAR_WIDTH, this.getY() + (int) (thumbY + thumbHeight), thumbColor);
    }

    // --- SHARED LAYOUT METHODS ---
    @Override
    public int getRowWidth() {
        // Make the list entries 3/5 of the screen's width.
        return (int) (this.parentScreen.width * 0.6);
    }

    @Override
    protected int getScrollbarX() {
        int listWidth = getRowWidth();
        int xStart = (this.width - listWidth) / 2; // center the list
        return xStart + listWidth + 4; // small padding from edge
    }


    // --- SHARED UTILITY OVERRIDES ---
    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        // We don't need narrations for this UI.
    }

    protected void handleEntryClick(E entry) {
        long now = System.currentTimeMillis();
        if (entry == this.lastClickedEntry && (now - this.lastClickTime) <= DOUBLE_CLICK_MS) {
            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
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

    public static abstract class Entry<E extends Entry<E>> extends EntryListWidget.Entry<E> {
    }

}