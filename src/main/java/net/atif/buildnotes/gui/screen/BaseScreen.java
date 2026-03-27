package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseScreen extends Screen {

    protected final Screen parent;

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // We only need to draw the gradient, as the world is always rendered behind it.
        context.fillGradient(0, 0, this.width, this.height, Colors.GRADIENT_START, Colors.GRADIENT_END);
    }

    /**
     * Default close behaviour: stop repeat events and return to parent screen.
     * Subclasses may override if they need different behaviour, but calling super.close()
     * will still perform the default.
     */
    @Override
    public void onClose() {
        if (this.minecraft != null) {
            open(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Convenience to open another screen safely.
     */
    protected void open(Screen next) {
        this.minecraft.setScreen(next);
    }

    /**
     * Convenience to show a standard confirm dialog whose cancel action returns to this screen.
     */
    protected void showConfirm(Component message, Runnable onConfirm) {
        UIHelper.showConfirmDialog(this, message, onConfirm);
    }
}
