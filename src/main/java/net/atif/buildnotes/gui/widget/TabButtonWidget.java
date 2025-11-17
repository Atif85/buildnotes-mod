package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TabButtonWidget extends ButtonWidget {

    private boolean isActive = false;

    public TabButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Determine the background color based on the button's state
        int color;
        if (this.isActive) {
            color = Colors.PANEL_BACKGROUND;
        } else if (this.isHovered()) {
            color = Colors.BUTTON_HOVER;
        } else {
            color = Colors.TAB_INACTIVE;
        }

        // Render the background
        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        // Render the text
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, Colors.TEXT_PRIMARY);
    }

    public boolean isActive() {
        return this.isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}