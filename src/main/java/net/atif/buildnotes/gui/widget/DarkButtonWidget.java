package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class DarkButtonWidget extends ButtonWidget {

    public DarkButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int color;
        if (!this.active) {
            color = Colors.BUTTON_DISABLED;
        } else if (this.isHovered()) {
            color = Colors.BUTTON_HOVER;
        } else {
            color = Colors.PANEL_BACKGROUND;
        }

        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = this.active ? Colors.TEXT_PRIMARY : Colors.TEXT_DISABLED;

        context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }
}
