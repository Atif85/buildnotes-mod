package net.atif.buildnotes.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public abstract class ScrollableScreen extends BaseScreen {

    protected final List<Element> scrollableWidgets = Lists.newArrayList();
    protected double scrollY = 0.0;
    protected int totalContentHeight = 0;

    // Scrolling mechanics
    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PADDING = 2;

    protected ScrollableScreen(Text title, Screen parent) {
        super(title, parent);
    }

    protected abstract void initContent();
    protected abstract void renderContent(DrawContext context, int mouseX, int mouseY, float delta);
    protected abstract int getTopMargin();
    protected abstract int getBottomMargin();

    @Override
    protected void init() {
        super.init();
        this.scrollableWidgets.clear();
        this.scrollY = 0;
        initContent();
    }

    /**
     * Type-safe addSelectableChild helper for scrollable widgets.
     */
    protected <T extends Element & Selectable> T addScrollableWidget(T widget) {
        this.scrollableWidgets.add(widget);
        return this.addSelectableChild(widget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderBackground(context);

        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        if (bottom <= top) return;

        double scale = this.client.getWindow().getScaleFactor();
        RenderSystem.enableScissor(
                (int)(0 * scale),
                (int)(this.client.getWindow().getFramebufferHeight() - (bottom * scale)),
                (int)(this.width * scale),
                (int)((bottom - top) * scale)
        );

        context.getMatrices().push();
        // 1. Move origin to the top of the scrollable area.
        context.getMatrices().translate(0.0, (double)top, 0.0);
        // 2. Move origin up by the scroll amount.
        context.getMatrices().translate(0.0, -this.scrollY, 0.0);

        int adjustedMouseY = (int)(mouseY - top + this.scrollY);
        this.renderContent(context, mouseX, adjustedMouseY, delta);

        for (Element widget : this.scrollableWidgets) {
            if (widget instanceof Drawable drawable) {
                drawable.render(context, mouseX, adjustedMouseY, delta);
            }
        }

        context.getMatrices().pop();
        RenderSystem.disableScissor();

        renderScrollbar(context);
    }

    private void renderScrollbar(DrawContext context) {
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        int trackHeight = bottom - top;
        int maxScroll = getMaxScroll();

        if (maxScroll > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
            float thumbHeight = Math.max(20, (trackHeight / (float)this.totalContentHeight) * trackHeight);
            float thumbY = (float) ((this.scrollY / maxScroll) * (trackHeight - thumbHeight));

            int color = isDraggingScrollbar ? 0xFFFFFFFF : 0x88FFFFFF;
            context.fill(scrollbarX, top + (int)thumbY, scrollbarX + SCROLLBAR_WIDTH, top + (int)(thumbY + thumbHeight), color);
        }
    }

    private int getMaxScroll() {
        int visibleHeight = this.height - getTopMargin() - getBottomMargin();
        return Math.max(0, this.totalContentHeight - visibleHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();

        // First, offer the scroll event to child widgets under the cursor
        if (mouseY >= top && mouseY < bottom) {
            double adjustedMouseY = mouseY - top + this.scrollY;
            for (Element widget : this.scrollableWidgets) {
                if (widget.isMouseOver(mouseX, adjustedMouseY)) {
                    if (widget.mouseScrolled(mouseX, adjustedMouseY, amount)) {
                        return true;
                    }
                }
            }
        }

        // If no child consumed it, scroll the main panel
        if (mouseY >= top && mouseY < bottom) {
            this.scrollY -= amount * 10;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element child : children()) {
            if (!scrollableWidgets.contains(child)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(child);
                    if (button == 0) {
                        this.setDragging(true);
                    }
                    return true;
                }
            }
        }

        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        int scrollbarX = this.width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;

        if (mouseX >= scrollbarX && mouseX < this.width && mouseY >= top && mouseY < bottom) {
            isDraggingScrollbar = true;
            return true;
        }

        if (mouseY >= top && mouseY < bottom) {
            double adjustedMouseY = mouseY - top + scrollY;
            for (Element widget : this.scrollableWidgets) {
                if (widget.mouseClicked(mouseX, adjustedMouseY, button)) {
                    this.setFocused(widget);
                    if (button == 0) this.setDragging(true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // Ensure consistent coordinate space for the dragged element
        if (this.getFocused() != null && this.isDragging() && button == 0) {
            // If the dragged element is scrollable, pass it the adjusted coordinates
            if (this.scrollableWidgets.contains(this.getFocused())) {
                double adjustedMouseY = mouseY - getTopMargin() + this.scrollY;
                return this.getFocused().mouseDragged(mouseX, adjustedMouseY, button, deltaX, deltaY);
            }
            // Otherwise, pass normal coordinates (for fixed buttons, etc.)
            else {
                return this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }
        }

        if (isDraggingScrollbar) {
            int trackHeight = (this.height - getTopMargin() - getBottomMargin());
            if (trackHeight <= 0) return true;

            float thumbHeight = Math.max(20, (trackHeight / (float)this.totalContentHeight) * trackHeight);
            if (trackHeight - thumbHeight <= 0) return true;

            double scrollRatio = (double)getMaxScroll() / (trackHeight - thumbHeight);
            this.scrollY += deltaY * scrollRatio;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 265) { // Up Arrow
            this.scrollY -= 10; clampScroll(); return true;
        } else if (keyCode == 264) { // Down Arrow
            this.scrollY += 10; clampScroll(); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void clampScroll() {
        if (this.scrollY < 0) this.scrollY = 0;
        int maxScroll = getMaxScroll();
        if (this.scrollY > maxScroll) this.scrollY = maxScroll;
    }
}