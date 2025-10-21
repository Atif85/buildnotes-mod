package net.atif.buildnotes.gui.widget.list;

import com.mojang.blaze3d.systems.RenderSystem;
import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;

public abstract class AbstractListWidget<E extends EntryListWidget.Entry<E>> extends EntryListWidget<E> {

    protected final MainScreen parentScreen;
    private boolean visible = false;
    private static final int FADE_HEIGHT = 12;

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
        if (!this.visible) {
            return false; // Ignore clicks if not visible
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

        // --- DRAW THE LIST ---
        super.render(matrices, mouseX, mouseY, delta);

        // --- DRAW FADE MASKS ---
        RenderSystem.disableScissor();  // turn off clipping to draw the fade overlays properly
        // Save the current state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Top fade overlay
        int left = (this.width - getRowWidth()) / 2;
        int right = left + getRowWidth();
        int topY = this.top;
        drawVerticalGradient(matrices, left, topY, right, topY + FADE_HEIGHT,
                0x60000000, 0x00000000);  // semi-transparent black to transparent

        // Bottom fade overlay
        int bottomY = this.bottom - FADE_HEIGHT;
        drawVerticalGradient(matrices, left, bottomY, right, this.bottom,
                0x00000000, 0x60000000); // transparent to semi-transparent black

        RenderSystem.disableBlend();
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
}