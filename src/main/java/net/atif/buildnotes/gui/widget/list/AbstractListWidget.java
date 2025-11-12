package net.atif.buildnotes.gui.widget.list;

import com.mojang.blaze3d.systems.RenderSystem;
import net.atif.buildnotes.gui.screen.MainScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;
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
        super(client, parent.width, parent.height, top, bottom, itemHeight);
        this.parentScreen = parent;

        // Disable default backgrounds.
        this.setRenderBackground(false);
        this.setRenderHorizontalShadows(false);
    }

    // --- SHARED VISIBILITY LOGIC ---
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        if (this.isMouseOver(mouseX, mouseY) && mouseX >= this.getScrollbarPositionX() && mouseX < this.getScrollbarPositionX() + SCROLLBAR_WIDTH) {
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
            int trackHeight = this.bottom - this.top;
            float maxScroll = this.getMaxScroll();
            float thumbHeight = Math.max(10, (float)(trackHeight * trackHeight) / (float)this.getMaxPosition());
            float draggableHeight = trackHeight - thumbHeight;

            if (draggableHeight <= 0) return true; // Cannot drag if thumb fills the track

            // Calculate the ratio of scrollable content to draggable area
            float ratio = maxScroll / draggableHeight;

            // Update scroll amount based on mouse movement since the drag started
            double relativeMouseY = mouseY - this.scrollbarDragStartMouseY;
            this.setScrollAmount(this.getScrollAmount() + (relativeMouseY * ratio));

            // Update the start position for the next frame's calculation
            this.scrollbarDragStartMouseY = mouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.visible) {
            return false; // If the widget is not visible, don't handle the scroll event.
        }
        // If it is visible, let the parent class handle the scrolling as usual.
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        // --- START SCISSORING (clip area so content doesn't render above or below list) ---
        double scale = MinecraftClient.getInstance().getWindow().getScaleFactor();

        // Convert logical screen coordinates to framebuffer coordinates
        int scissorX = (int) (0 * scale);
        int scissorY = (int) (this.client.getWindow().getFramebufferHeight() - (this.bottom * scale));
        int scissorWidth = (int) (this.width * scale);
        int scissorHeight = (int) ((this.bottom - this.top) * scale);

        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        super.render(matrices, mouseX, mouseY, delta);
        renderCustomScrollbar(matrices); // We call this instead of relying on super's scrollbar

        RenderSystem.disableScissor();  // turn off clipping to draw the fade overlays properly

        // Save the current state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Top fade overlay
        int left = this.left;
        int right = this.right;
        int topY = this.top;
        drawVerticalGradient(matrices, left, topY, right, topY + FADE_HEIGHT,
                0x60000000, 0x00000000);  // semi-transparent black to transparent

        // Bottom fade overlay
        int bottomY = this.bottom - FADE_HEIGHT;
        drawVerticalGradient(matrices, left, bottomY, right, this.bottom,
                0x00000000, 0x60000000); // transparent to semi-transparent black

        RenderSystem.disableBlend();
    }

    protected void renderCustomScrollbar(MatrixStack matrices) {
        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) return; // Don't render if not scrollable

        int scrollbarX = this.getScrollbarPositionX();
        int trackHeight = this.bottom - this.top;

        float thumbHeight = Math.max(10, (float)(trackHeight * trackHeight) / (float)this.getMaxPosition());
        float thumbY = (float)this.getScrollAmount() / (float)(this.getMaxPosition() - trackHeight) * (trackHeight - thumbHeight);

        int thumbColor = isDraggingScrollbar ? 0xFFFFFFFF : 0x88FFFFFF;

        fill(matrices, scrollbarX, this.top + (int) thumbY, scrollbarX + SCROLLBAR_WIDTH, this.top + (int) (thumbY + thumbHeight), thumbColor);
    }

    private void drawVerticalGradient(MatrixStack matrices, int x1, int y1, int x2, int y2, int topColor, int bottomColor) {
        fillGradient(matrices, x1, y1, x2, y2, topColor, bottomColor);
    }

    // --- SHARED LAYOUT METHODS ---
    @Override
    public int getRowWidth() {
        // Make the list entries 3/5 of the screen's width.
        return (int) (this.parentScreen.width * 0.6);
    }

    @Override
    protected int getScrollbarPositionX() {
        int listWidth = getRowWidth();
        int xStart = (this.width - listWidth) / 2; // center the list
        return xStart + listWidth + 4; // small padding from edge
    }


    // --- SHARED UTILITY OVERRIDES ---
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
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
        // not a double-click, register this as the last click
        this.lastClickedEntry = entry;
        this.lastClickTime = now;
    }

    public static abstract class Entry<E extends Entry<E>> extends EntryListWidget.Entry<E> {
    }

}