package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.NoteScreenLayouts;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;
import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.Scope;
import net.minecraft.client.gui.Element;
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

        boolean canEdit = !(this.note.getScope() == Scope.SERVER && !ClientSession.hasEditPermission());

        int buttonsY = UIHelper.getBottomButtonY(this);
        UIHelper.createButtonRow(this, buttonsY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> {
                    DarkButtonWidget deleteButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT, new TranslatableText("gui.buildnotes.delete_button"), button -> confirmDelete());
                    deleteButton.active = canEdit;
                    this.addDrawableChild(deleteButton);
                }
                case 1 -> {
                    DarkButtonWidget editButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT, new TranslatableText("gui.buildnotes.edit_button"), button -> this.client.setScreen(new EditNoteScreen(this.parent, this.note)));
                    editButton.active = canEdit;
                    this.addDrawableChild(editButton);
                }
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new TranslatableText("gui.buildnotes.close_button"), button -> this.client.setScreen(parent)));
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
        UIHelper.showConfirmDialog(this, new LiteralText("Delete note \"" + note.getTitle() + "\"?"), onConfirm);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginSingleRow();

        // --- Title Panel ---
        UIHelper.drawPanel(matrices, contentX, NoteScreenLayouts.TOP_MARGIN, contentWidth, NoteScreenLayouts.TITLE_PANEL_HEIGHT);
        this.titleArea.render(matrices, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Find which child element was clicked and set it as the "focused" one for this interaction.
        for (Element element : this.children()) {
            if (element.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(element);
                if (button == 0) {
                    this.setDragging(true);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // If we are in a drag operation, send the event directly to the focused element.
        if (this.getFocused() != null && this.isDragging() && button == 0) {
            return this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // This is the crucial fix. The mouse release event is sent to the element
        // that was focused during mouseClicked, not the one currently under the cursor.
        if (this.getFocused() != null && this.isDragging() && button == 0) {
            this.setDragging(false);
            return this.getFocused().mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}