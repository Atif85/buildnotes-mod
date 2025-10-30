package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class ViewNoteScreen extends BaseScreen {

    private final Note note;

    private ReadOnlyMultiLineTextFieldWidget titleArea;
    private ReadOnlyMultiLineTextFieldWidget contentArea;

    public ViewNoteScreen(Screen parent, Note note) {
        super(new LiteralText(note.getTitle()), parent);
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        int buttonsY = this.height - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;
        UIHelper.createBottomButtonRow(this, buttonsY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new TranslatableText("gui.buildnotes.delete_button"), button -> confirmDelete()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new TranslatableText("gui.buildnotes.edit_button"), button -> this.client.setScreen(new EditNoteScreen(this.parent, this.note))));
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new TranslatableText("gui.buildnotes.close_button"), button -> this.client.setScreen(parent)));
            }
        });

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
            this.close();
        };
        UIHelper.showConfirmDialog(this, new LiteralText("Delete note \"" + note.getTitle() + "\"?"), onConfirm);
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
        UIHelper.drawPanel(matrices, contentX, topMargin, contentWidth, titlePanelHeight);
        this.titleArea.render(matrices, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        UIHelper.drawPanel(matrices, contentX, contentPanelY, contentWidth, contentPanelBottom - contentPanelY);
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
}