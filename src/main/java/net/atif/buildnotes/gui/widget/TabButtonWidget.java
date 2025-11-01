package net.atif.buildnotes.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class TabButtonWidget extends ButtonWidget {

    private boolean isActive = false;

    public TabButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Determine the background color based on the button's state
        int color;
        if (this.isActive) {
            // A brighter, less transparent color for the active tab
            color = 0x77000000;
        } else if (this.isHovered()) {
            // A slightly brighter color when hovered
            color = 0xAA000000;
        } else {
            // Default dark, translucent background
            color = 0x44000000;
        }

        // Render the background
        fill(matrices, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        // Render the text
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = 0xFFFFFF;
        drawCenteredTextWithShadow(matrices, textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}