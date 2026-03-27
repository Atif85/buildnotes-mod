package net.atif.buildnotes.gui.widget;

import net.atif.buildnotes.gui.helper.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;

public class TabButtonWidget extends Button {

    private boolean selected = false;

    public TabButtonWidget(int x, int y, int width, int height, MutableComponent message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Determine the background color based on the button's state
        int color;
        if (this.selected) {
            color = Colors.PANEL_BACKGROUND;
        } else if (this.isHovered()) {
            color = Colors.BUTTON_HOVER;
        } else {
            color = Colors.TAB_INACTIVE;
        }

        // Render the background
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, color);

        // Render the text
        Font font = Minecraft.getInstance().font;
        graphics.centeredText(font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, Colors.TEXT_PRIMARY);
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}