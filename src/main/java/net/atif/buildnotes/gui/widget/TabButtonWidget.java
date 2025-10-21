package net.atif.buildnotes.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.UUID;

import static net.minecraft.client.gui.DrawableHelper.fill;

public class TabButtonWidget extends ButtonWidget {

    private boolean isActive = false;

    public TabButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // Determine the background color based on the button's state
        int color;
        if (this.isActive) {
            // A brighter, less transparent color for the active tab
            color = 0x99FFFFFF;
        } else if (this.isHovered()) {
            // A slightly brighter color when hovered
            color = 0x80FFFFFF;
        } else {
            // Default dark, translucent background
            color = 0x60000000;
        }

        // Render the background
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, color);

        // Render the text
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textColor = 0xFFFFFF;
        drawCenteredText(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}