package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class DarkButtonWidget extends ButtonWidget {

    public DarkButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int color;
        if (!this.active) {
            color = Colors.BUTTON_DISABLED;
        } else if (this.isHovered()) {
            color = Colors.BUTTON_HOVER;
        } else {
            color = Colors.PANEL_BACKGROUND;
        }

        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, color);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = this.active ? Colors.TEXT_PRIMARY : Colors.TEXT_DISABLED;
        drawCenteredText(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }
}
