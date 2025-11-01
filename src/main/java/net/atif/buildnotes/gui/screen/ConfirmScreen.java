package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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

        int btnY = panelY + panelH - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;

        UIHelper.createBottomButtonRow(this, btnY, 2, x -> {
            boolean isLeft = x == UIHelper.getCenteredButtonStartX(this.width, 2);
            this.addDrawableChild(new DarkButtonWidget(
                    x, btnY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                    Text.of(isLeft ? "Yes" : "No"),
                    b -> { if (isLeft) onConfirm.run(); else onCancel.run(); }
            ));
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int panelW = Math.min(this.width - 80, 360);
        int panelH = 100;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        UIHelper.drawPanel(context, panelX, panelY, panelW, panelH);

        context.drawTextWithShadow(this.textRenderer, this.message, panelX + 12, panelY + 12, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }
}
