package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.TabType;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class ViewNoteScreen extends Screen {

    private final Screen parent;
    private final Note note;
    private ReadOnlyMultiLineTextFieldWidget titleArea;
    private ReadOnlyMultiLineTextFieldWidget contentArea;

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

        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.delete_button"),
                button -> confirmDelete()
        ));

        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.edit_button"),
                button -> this.client.setScreen(new EditNoteScreen(this.parent, this.note))
        ));

        this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 2, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.close_button"),
                button -> this.client.setScreen(parent)
        ));

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int topMargin = 20;
        int titlePanelHeight = 25;
        int panelSpacing = 5;
        int bottomMargin = 45;

        // --- Title Widget ---
        this.titleArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer,
                contentX,
                topMargin + 5,
                contentWidth,
                titlePanelHeight,
                this.note.getTitle(),
                1,
                false
        );
        this.addSelectableChild(this.titleArea);

        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        int contentPanelHeight = contentPanelBottom - contentPanelY;

        // --- Content Widget ---
        this.contentArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer,
                contentX,
                contentPanelY,
                contentWidth,
                contentPanelHeight,
                note.getContent(),
                Integer.MAX_VALUE,
                true
        );
        this.addSelectableChild(this.contentArea);
    }

    private void confirmDelete() {
        Runnable onConfirm = () -> {
            DataManager.getInstance().deleteNote(this.note);
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
        int bottomMargin = 45;

        // --- Title Panel ---
        fill(matrices, contentX, topMargin, contentX + contentWidth, topMargin + titlePanelHeight, 0x77000000);
        this.titleArea.render(matrices, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        fill(matrices, contentX, contentPanelY, contentX + contentWidth, contentPanelBottom, 0x77000000);

        this.contentArea.render(matrices, mouseX, mouseY, delta);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Pass key events to both widgets for copying/navigation
        if (this.titleArea.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.contentArea.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // --- Delegate scrolling to the widget ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Pass scroll events to the widget under the mouse
        if (this.titleArea.isMouseOver(mouseX, mouseY)) {
            return this.titleArea.mouseScrolled(mouseX, mouseY, amount);
        }
        if (this.contentArea.isMouseOver(mouseX, mouseY)) {
            return this.contentArea.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() { this.client.setScreen(this.parent); }
}