package net.atif.buildnotes.gui.widget;

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
            // disabled less dark
            color = 0x44000000;
        } else if (this.isHovered()) {
            // hover darker
            color = 0xAA000000;
        } else {
            // default dark translucent
            color = 0x77000000;
        }

        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, color);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = this.active ? 0xFFFFFF : 0x888888;
        drawCenteredText(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }
}
