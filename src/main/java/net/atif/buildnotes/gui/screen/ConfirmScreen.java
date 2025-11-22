package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfirmScreen extends BaseScreen {

    private final Text message;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmScreen(Screen parent, Text message, Runnable onConfirm, Runnable onCancel) {
        super(Text.of("Confirm"), parent);
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
            this.addDrawableChild(new DarkButtonWidget(
                    x, btnY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                    Text.of(isLeft ? "Yes" : "No"),
                    b -> { if (isLeft) onConfirm.run(); else onCancel.run(); }
            ));
        });
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int panelW = Math.min(this.width - 80, 360);
        int panelH = 100;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        panelH = panelH - (UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2));
        UIHelper.drawPanel(matrices, panelX, panelY, panelW, panelH);

        int textMaxWidth = panelW - 24; // 12px padding on each side
        this.textRenderer.drawTrimmed(matrices, this.message, panelX + 12, panelY + 12, textMaxWidth, Colors.TEXT_PRIMARY);
    }
}
