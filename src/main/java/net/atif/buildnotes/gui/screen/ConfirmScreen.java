package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class ConfirmScreen extends Screen {

    private final Screen parent;
    private final Text message;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmScreen(Screen parent, Text message, Runnable onConfirm, Runnable onCancel) {

        super(new LiteralText("Confirm"));
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        super.init();
        int panelW = Math.min(this.width - 80, 360);
        int panelH = 100;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int btnW = 100;
        int btnH = 20;
        int spacing = 10;
        int leftBtnX = panelX + (panelW / 2) - btnW - (spacing / 2);
        int rightBtnX = panelX + (panelW / 2) + (spacing / 2);
        int btnY = panelY + panelH - btnH - 12;

        this.addDrawableChild(new DarkButtonWidget(leftBtnX, btnY, btnW, btnH, new LiteralText("Yes"), button -> {
            if (this.onConfirm != null) this.onConfirm.run();
        }));

        this.addDrawableChild(new DarkButtonWidget(rightBtnX, btnY, btnW, btnH, new LiteralText("No"), button -> {
            if (this.onCancel != null) this.onCancel.run();
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Dim the world behind
        this.renderBackground(matrices);

        // Draw centered panel
        int panelW = Math.min(this.width - 80, 360);
        int panelH = 100;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        // panel background - dark translucent
        fill(matrices, panelX, panelY, panelX + panelW, panelY + panelH, 0x6D000000);

        // message
        int textX = panelX + 12;
        int textY = panelY + 12;
        this.textRenderer.drawTrimmed(this.message, textX, textY, panelW - 24, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
