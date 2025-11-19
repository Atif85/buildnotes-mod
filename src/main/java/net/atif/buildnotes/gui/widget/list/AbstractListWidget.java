package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.screen.MainScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
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
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!this.visible) return false;


        double mouseX = click.x();
        double mouseY = click.y();

        if (this.isMouseOver(mouseX, mouseY) && mouseX >= this.getScrollbarX() && mouseX < this.getScrollbarX() + SCROLLBAR_WIDTH) {
            this.isDraggingScrollbar = true;
            return true;
        }

        this.isDraggingScrollbar = false;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        isDraggingScrollbar = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (this.isDraggingScrollbar) {
            int trackHeight = this.getHeight();
            float maxScroll = this.getMaxScrollY();
            if (maxScroll <= 0) return true;

            float thumbHeight = Math.max(10, (float)(trackHeight * trackHeight) / (float)this.getContentsHeightWithPadding());
            float draggableHeight = trackHeight - thumbHeight;
            if (draggableHeight <= 0) return true; // Cannot drag if thumb fills the track

            // Calculate the ratio of scrollable content to draggable area
            float ratio = maxScroll / draggableHeight;

            this.setScrollY(this.getScrollY() + (offsetY * ratio));

            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
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
                0x60000000, 0x00000000);

        int bottomY = this.getBottom() - FADE_HEIGHT;
        context.fillGradient(left, bottomY, right, this.getBottom(),
                0x00000000, 0x60000000);

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