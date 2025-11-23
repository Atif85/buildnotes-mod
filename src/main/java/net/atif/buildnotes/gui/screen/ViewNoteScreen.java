package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.NoteScreenLayouts;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.Scope;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ViewNoteScreen extends BaseScreen {

    private final Note note;

    private ReadOnlyMultiLineTextFieldWidget titleArea;
    private ReadOnlyMultiLineTextFieldWidget contentArea;

    public ViewNoteScreen(Screen parent, Note note) {
        super(Text.translatable(note.getTitle()), parent);
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        boolean canEdit = !(this.note.getScope() == Scope.SERVER && !ClientSession.hasEditPermission());

        int buttonsY = UIHelper.getBottomButtonY(this);
        UIHelper.createButtonRow(this, buttonsY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> {
                    DarkButtonWidget deleteButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT, Text.translatable("gui.buildnotes.delete_button"), button -> confirmDelete());
                    deleteButton.active = canEdit;
                    this.addDrawableChild(deleteButton);
                }
                case 1 -> {
                    DarkButtonWidget editButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT, Text.translatable("gui.buildnotes.edit_button"), button -> this.client.setScreen(new EditNoteScreen(this.parent, this.note)));
                    editButton.active = canEdit;
                    this.addDrawableChild(editButton);
                }
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.close_button"), button -> this.client.setScreen(parent)));
            }
        });

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginSingleRow();

        // --- Title Widget ---
        this.titleArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer,
                contentX,
                NoteScreenLayouts.TOP_MARGIN + 5,
                contentWidth,
                NoteScreenLayouts.TITLE_PANEL_HEIGHT,
                this.note.getTitle(),
                1,
                false
        );
        this.addSelectableChild(this.titleArea);

        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
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
        UIHelper.showConfirmDialog(this, Text.translatable("Delete note \"" + note.getTitle() + "\"?"), onConfirm);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginSingleRow();

        // --- Title Panel ---
        UIHelper.drawPanel(context, contentX, NoteScreenLayouts.TOP_MARGIN, contentWidth, NoteScreenLayouts.TITLE_PANEL_HEIGHT);
        this.titleArea.render(context, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
        int contentPanelBottom = this.height - bottomMargin;
        UIHelper.drawPanel(context, contentX, contentPanelY, contentWidth, contentPanelBottom - contentPanelY);
        this.contentArea.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
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