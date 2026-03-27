package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class DarkButtonWidget extends Button {

    public DarkButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int color;
        if (!this.active) {
            color = Colors.BUTTON_DISABLED;
        } else if (this.isHovered()) {
            color = Colors.BUTTON_HOVER;
        } else {
            color = Colors.PANEL_BACKGROUND;
        }

        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        Font font = Minecraft.getInstance().font;
        int textColor = this.active ? Colors.TEXT_PRIMARY : Colors.TEXT_DISABLED;

        graphics.centeredText(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }
}
