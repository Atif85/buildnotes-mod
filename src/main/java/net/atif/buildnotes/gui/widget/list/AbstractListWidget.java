package net.atif.buildnotes.gui.widget.list;

import net.atif.buildnotes.gui.screen.MainScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;

public abstract class AbstractListWidget<E extends EntryListWidget.Entry<E>> extends EntryListWidget<E> {

    protected final MainScreen parentScreen;
    private boolean visible = false;

    public AbstractListWidget(MainScreen parent, MinecraftClient client, int top, int bottom, int itemHeight) {
        // We pass the parent's width and height to the super constructor.
        super(client, parent.width, parent.height, top, bottom, itemHeight);
        this.parentScreen = parent;

        int rowWidth = this.getRowWidth();
        this.left = (this.parentScreen.width / 2) - (rowWidth / 2);
        this.right = this.left + rowWidth;

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
        if (!this.visible) {
            return; // Don't render if not visible
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    // --- SHARED LAYOUT METHODS ---
    @Override
    public int getRowWidth() {
        // Make the list entries 3/5 of the screen's width.
        return this.parentScreen.width - 80;
    }

    @Override
    protected int getScrollbarPositionX() {
        // Position the scrollbar to the right of the row.
        return this.left + this.getRowWidth() + 6;
    }

    // --- SHARED UTILITY OVERRIDES ---
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
        // We don't need narrations for this UI.
    }
}