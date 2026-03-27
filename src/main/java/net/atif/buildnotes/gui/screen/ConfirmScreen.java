package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmScreen extends BaseScreen {

    private final Component message;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmScreen(Screen parent, Component message, Runnable onConfirm, Runnable onCancel) {
        super(Component.literal("Confirm"), parent);
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        super.init();

        int panelH = 100;
        int panelY = (this.height - panelH) / 2;

        int btnY = panelY + panelH - UIHelper.BUTTON_HEIGHT - UIHelper.OUTER_PADDING;

        UIHelper.createButtonRow(this, btnY, 2, x -> {
            boolean isLeft = x == UIHelper.getCenteredButtonStartX(this.width, 2);
            this.addRenderableWidget(new DarkButtonWidget(
                    x, btnY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                    Component.literal(isLeft ? "Yes" : "No"),
                    b -> { if (isLeft) onConfirm.run(); else onCancel.run(); }
            ));
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int panelW = Math.min(this.width - 80, 360);
        int panelH = 100;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        panelH = panelH - (UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2));
        UIHelper.drawPanel(graphics, panelX, panelY, panelW, panelH);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
 
        int textMaxWidth = panelW - 24; // 12px padding on each side
        String trimmedMessage = this.font.plainSubstrByWidth(this.message.getString(), textMaxWidth);
        graphics.text(this.font, Component.literal(trimmedMessage), panelX + 12, panelY + 12, Colors.TEXT_PRIMARY);
    }
}
