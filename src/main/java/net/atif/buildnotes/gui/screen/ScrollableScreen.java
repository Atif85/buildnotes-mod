package net.atif.buildnotes.gui.screen;

import com.google.common.collect.Lists;
import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.List;

public abstract class ScrollableScreen extends BaseScreen {

    protected final List<GuiEventListener> scrollableWidgets = Lists.newArrayList();
    protected double scrollY = 0.0;
    protected int totalContentHeight = 0;

    // Scrolling mechanics
    private boolean isDraggingScrollbar = false;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PADDING = 2;

    protected ScrollableScreen(Component title, Screen parent) {
        super(title, parent);
    }

    protected abstract void initContent();
    protected abstract void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);
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
    protected <T extends GuiEventListener & NarratableEntry> void addScrollableWidget(T widget) {
        this.scrollableWidgets.add(widget);
        this.addWidget(widget);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        if (bottom <= top) return;


        graphics.enableScissor( 0, top, this.width, bottom);

        Matrix3x2fStack matrices = graphics.pose();

        matrices.pushMatrix();
        // 1. Move origin to the top of the scrollable area.
        matrices.translate(0.0f, (float) top);
        // 2. Move origin up by the scroll amount.
        matrices.translate(0.0f, (float) -this.scrollY);

        int adjustedMouseY = (int)(mouseY - top + this.scrollY);
        this.renderContent(graphics, mouseX, adjustedMouseY, delta);

        for (GuiEventListener widget : this.scrollableWidgets) {
            if (widget instanceof Renderable drawable) {
                drawable.extractRenderState(graphics, mouseX, adjustedMouseY, delta);
            }
        }

        matrices.popMatrix();
        graphics.disableScissor();

        renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        int trackHeight = bottom - top;
        int maxScroll = getMaxScroll();

        if (maxScroll > 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
            float thumbHeight = Math.max(20, (trackHeight / (float)this.totalContentHeight) * trackHeight);
            float thumbY = (float) ((this.scrollY / maxScroll) * (trackHeight - thumbHeight));

            int color = isDraggingScrollbar ? Colors.SCROLLBAR_THUMB_ACTIVE : Colors.SCROLLBAR_THUMB_INACTIVE;
            graphics.fill(scrollbarX, top + (int)thumbY, scrollbarX + SCROLLBAR_WIDTH, top + (int)(thumbY + thumbHeight), color);
        }
    }

    private int getMaxScroll() {
        int visibleHeight = this.height - getTopMargin() - getBottomMargin();
        return Math.max(0, this.totalContentHeight - visibleHeight);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();

        // First, offer the scroll event to child widgets under the cursor
        if (mouseY >= top && mouseY < bottom) {
            double adjustedMouseY = mouseY - top + this.scrollY;
            for (GuiEventListener widget : this.scrollableWidgets) {
                if (widget.isMouseOver(mouseX, adjustedMouseY)) {
                    if (widget.mouseScrolled(mouseX, adjustedMouseY, horizontalAmount, verticalAmount)) {
                        return true;
                    }
                }
            }
        }

        // If no child consumed it, scroll the main panel
        if (mouseY >= top && mouseY < bottom) {
            this.scrollY -= verticalAmount * 10;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        for (GuiEventListener child : children()) {
            if (!scrollableWidgets.contains(child)) {
                if (child.mouseClicked(event, doubled)) {
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
            MouseButtonEvent adjustedEvent = new MouseButtonEvent(mouseX, adjustedMouseY, event.buttonInfo());

            for (GuiEventListener widget : this.scrollableWidgets) {
                if (widget.mouseClicked(adjustedEvent, doubled)) {
                    this.setFocused(widget);
                    if (button == 0) this.setDragging(true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (this.getFocused() != null && this.isDragging() && button == 0) {
            // Create a new Click object with adjusted coordinates for scrollable widgets
            if (this.scrollableWidgets.contains(this.getFocused())) {
                double adjustedMouseY = mouseY - getTopMargin() + this.scrollY;
                MouseButtonEvent adjustedEvent = new MouseButtonEvent(mouseX, adjustedMouseY, event.buttonInfo());
                return this.getFocused().mouseDragged(adjustedEvent, offsetX, offsetY);
            } else {
                // Pass original event to fixed widgets
                return this.getFocused().mouseDragged(event, offsetX, offsetY);
            }
        }

        if (isDraggingScrollbar) {
            int trackHeight = (this.height - getTopMargin() - getBottomMargin());
            if (trackHeight <= 0) return true;

            float thumbHeight = Math.max(20, (trackHeight / (float)this.totalContentHeight) * trackHeight);
            if (trackHeight - thumbHeight <= 0) return true;

            double scrollRatio = (double)getMaxScroll() / (trackHeight - thumbHeight);
            this.scrollY += offsetY * scrollRatio;
            clampScroll();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // First, handle the screen's own scrollbar state.
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }


        if (this.getFocused() != null) {
            boolean handled;
            // Adjust coordinates for scrollable widgets
            if (this.scrollableWidgets.contains(this.getFocused())) {
                double adjustedMouseY = event.y() - getTopMargin() + this.scrollY;
                MouseButtonEvent adjustedEvent = new MouseButtonEvent(event.x(), adjustedMouseY, event.buttonInfo());
                handled = this.getFocused().mouseReleased(adjustedEvent);
            } else {
                handled = this.getFocused().mouseReleased(event);
            }

            this.setDragging(false);
            if (handled) {
                return true;
            }
        }

        // Use the new method on the superclass
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.getFocused() != null && this.getFocused().keyPressed(event)) {
            return true;
        }
        // Use GLFW constants for key codes
        if (event.isUp()) {
            this.scrollY -= 10; clampScroll(); return true;
        } else if (event.isDown()) {
            this.scrollY += 10; clampScroll(); return true;
        }
        return super.keyPressed(event);
    }

    private void clampScroll() {
        if (this.scrollY < 0) this.scrollY = 0;
        int maxScroll = getMaxScroll();
        if (this.scrollY > maxScroll) this.scrollY = maxScroll;
    }
}