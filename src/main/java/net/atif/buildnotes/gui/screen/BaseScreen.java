package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.helper.UIHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class BaseScreen extends Screen {

    protected final Screen parent;

    protected BaseScreen(Text title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (this.client != null) {
            this.client.keyboard.setRepeatEvents(true);
        }
    }

    /**
     * Default close behaviour: stop repeat events and return to parent screen.
     * Subclasses may override if they need different behaviour, but calling super.close()
     * will still perform the default.
     */
    @Override
    public void close() {
        if (this.client != null) {
            this.client.keyboard.setRepeatEvents(false);
            this.client.setScreen(this.parent);
        } else {
            super.close();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * Convenience to open another screen safely.
     */
    protected void open(Screen next) {
        if (this.client != null) {
            this.client.setScreen(next);
        }
    }

    /**
     * Convenience to show a standard confirm dialog whose cancel action returns to this screen.
     */
    protected void showConfirm(net.minecraft.text.Text message, Runnable onConfirm) {
        UIHelper.showConfirmDialog(this, message, onConfirm);
    }
}
