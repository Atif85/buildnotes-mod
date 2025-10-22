package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.TabType;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.TranslatableText;
import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class ViewNoteScreen extends Screen {

    private final Screen parent;
    private final Note note;
    private List<OrderedText> wrappedContent;
    private double scrollY = 0;
    private int contentHeight = 0;

    public ViewNoteScreen(Screen parent, Note note) {
        super(new LiteralText(note.getTitle()));
        this.parent = parent;
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomPadding = 12;
        int buttonSpacing = 8;
        int totalButtonWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int buttonsStartX = (this.width - totalButtonWidth) / 2;
        int buttonsY = this.height - buttonHeight - bottomPadding;

        // Back Button
        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.close_button"),
                button -> this.client.setScreen(parent)
        ));

        // Edit Button
        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.edit_button"),
                button -> {
                    this.client.setScreen(new EditNoteScreen(this.parent, this.note));
                }
        ));

        // Delete Button
        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 2, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.delete_button"),
                button -> confirmDelete()
        ));

        // Wrap the note content based on the screen width
        int contentWidth = (int) (this.width * 0.6);
        this.wrappedContent = this.textRenderer.wrapLines(new LiteralText(note.getContent()), contentWidth - 20); // 10px padding on each side
        this.contentHeight = this.wrappedContent.size() * this.textRenderer.fontHeight;
    }

    private void confirmDelete() {
        Runnable onConfirm = () -> {
            DataManager dataManager = DataManager.getInstance();
            dataManager.getNotes().removeIf(n -> n.getId().equals(this.note.getId()));
            dataManager.saveNotes();
            // The parent screen (MainScreen) needs to be refreshed.
            this.client.setScreen(new MainScreen(TabType.NOTES));
        };
        Runnable onCancel = () -> this.client.setScreen(this);
        this.client.setScreen(new ConfirmScreen(this, new LiteralText("Delete note \"" + note.getTitle() + "\"?"), onConfirm, onCancel));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int topMargin = 20;
        int titlePanelHeight = 25;
        int panelSpacing = 5;
        int bottomMargin = 45; // Space for buttons

        // --- Title Panel ---
        fill(matrices, contentX, topMargin, contentX + contentWidth, topMargin + titlePanelHeight, 0x77000000);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, topMargin + (titlePanelHeight - 8) / 2, 0xFFFFFF);

        // --- Content Panel ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        fill(matrices, contentX, contentPanelY, contentX + contentWidth, contentPanelBottom, 0x77000000);

        // --- Scissoring for scrollable text ---
        // This clips the rendering so text doesn't draw outside the content panel
        double scale = this.client.getWindow().getScaleFactor();
        int scissorX = (int) (contentX * scale);
        int scissorY = (int) (this.client.getWindow().getFramebufferHeight() - (contentPanelBottom * scale));
        int scissorWidth = (int) (contentWidth * scale);
        int scissorHeight = (int) ((contentPanelBottom - contentPanelY) * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        // Render the wrapped text lines
        int textX = contentX + 10;
        int textStartY = contentPanelY + 10;
        for (int i = 0; i < wrappedContent.size(); i++) {
            int lineY = textStartY + (i * this.textRenderer.fontHeight) + (int)this.scrollY;
            // Only render visible lines
            if (lineY >= contentPanelY && lineY <= contentPanelBottom - this.textRenderer.fontHeight) {
                this.textRenderer.draw(matrices, wrappedContent.get(i), textX, lineY, 0xFFFFFF);
            }
        }

        // Stop clipping
        RenderSystem.disableScissor();

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int contentPanelHeight = (this.height - 45) - (20 + 25 + 5);
        int maxScroll = Math.max(0, this.contentHeight - contentPanelHeight + 20);

        // Invert amount because Minecraft's scroll is inverted
        this.scrollY -= amount * 10;
        if (this.scrollY > 0) {
            this.scrollY = 0;
        }
        if (this.scrollY < -maxScroll) {
            this.scrollY = -maxScroll;
        }

        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}