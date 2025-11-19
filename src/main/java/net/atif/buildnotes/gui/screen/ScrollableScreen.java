package net.atif.buildnotes.gui.screen;

import com.google.common.collect.Lists;
import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

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
    protected <T extends Element & Selectable> void addScrollableWidget(T widget) {
        this.scrollableWidgets.add(widget);
        this.addSelectableChild(widget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        if (bottom <= top) return;


        context.enableScissor( 0, top, this.width, bottom);

        Matrix3x2fStack matrices = context.getMatrices();

        matrices.pushMatrix();
        // 1. Move origin to the top of the scrollable area.
        matrices.translate(0.0f, (float) top);
        // 2. Move origin up by the scroll amount.
        matrices.translate(0.0f, (float) -this.scrollY);

        int adjustedMouseY = (int)(mouseY - top + this.scrollY);
        this.renderContent(context, mouseX, adjustedMouseY, delta);

        for (Element widget : this.scrollableWidgets) {
            if (widget instanceof Drawable drawable) {
                drawable.render(context, mouseX, adjustedMouseY, delta);
            }
        }

        matrices.popMatrix();
        context.disableScissor();

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

            int color = isDraggingScrollbar ? Colors.SCROLLBAR_THUMB_ACTIVE : Colors.SCROLLBAR_THUMB_INACTIVE;
            context.fill(scrollbarX, top + (int)thumbY, scrollbarX + SCROLLBAR_WIDTH, top + (int)(thumbY + thumbHeight), color);
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
            for (Element widget : this.scrollableWidgets) {
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
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        for (Element child : children()) {
            if (!scrollableWidgets.contains(child)) {
                if (child.mouseClicked(click, doubled)) {
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
            Click adjustedClick = new Click(mouseX, adjustedMouseY, click.buttonInfo());

            for (Element widget : this.scrollableWidgets) {
                if (widget.mouseClicked(adjustedClick, doubled)) {
                    this.setFocused(widget);
                    if (button == 0) this.setDragging(true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (this.getFocused() != null && this.isDragging() && button == 0) {
            // Create a new Click object with adjusted coordinates for scrollable widgets
            if (this.scrollableWidgets.contains(this.getFocused())) {
                double adjustedMouseY = mouseY - getTopMargin() + this.scrollY;
                Click adjustedClick = new Click(mouseX, adjustedMouseY, click.buttonInfo());
                return this.getFocused().mouseDragged(adjustedClick, offsetX, offsetY);
            } else {
                // Pass original click to fixed widgets
                return this.getFocused().mouseDragged(click, offsetX, offsetY);
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
    public boolean mouseReleased(Click click) {
        // First, handle the screen's own scrollbar state.
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }


        if (this.getFocused() != null) {
            boolean handled;
            // Adjust coordinates for scrollable widgets
            if (this.scrollableWidgets.contains(this.getFocused())) {
                double adjustedMouseY = click.y() - getTopMargin() + this.scrollY;
                Click adjustedClick = new Click(click.x(), adjustedMouseY, click.buttonInfo());
                handled = this.getFocused().mouseReleased(adjustedClick);
            } else {
                handled = this.getFocused().mouseReleased(click);
            }

            this.setDragging(false);
            if (handled) {
                return true;
            }
        }

        // Use the new method on the superclass
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.getFocused() != null && this.getFocused().keyPressed(input)) {
            return true;
        }
        // Use GLFW constants for key codes
        if (input.isUp()) {
            this.scrollY -= 10; clampScroll(); return true;
        } else if (input.isDown()) {
            this.scrollY += 10; clampScroll(); return true;
        }
        return super.keyPressed(input);
    }

    private void clampScroll() {
        if (this.scrollY < 0) this.scrollY = 0;
        int maxScroll = getMaxScroll();
        if (this.scrollY > maxScroll) this.scrollY = maxScroll;
    }
}