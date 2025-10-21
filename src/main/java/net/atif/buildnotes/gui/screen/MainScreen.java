package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.widget.TabButtonWidget;
import net.atif.buildnotes.gui.widget.list.BuildListWidget;
import net.atif.buildnotes.gui.widget.list.NoteListWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class MainScreen extends Screen {

    private TabButtonWidget notesTab;
    private TabButtonWidget buildsTab;
    private ButtonWidget editButton;
    private ButtonWidget deleteButton;

    private NoteListWidget noteListWidget;
    private BuildListWidget buildListWidget;

    public MainScreen() {
        super(new TranslatableText("gui.buildnotes.main_title"));
    }

    @Override
    protected void init() {
        super.init();
        this.client.keyboard.setRepeatEvents(true);

        // --- TABS ---
        int tabWidth = 80;
        int tabHeight = 20;
        int topMargin = 40; // Space for title and tabs
        int bottomMargin = 40; // Space for action buttons

        this.notesTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) - tabWidth - 2, 15, tabWidth, tabHeight,
                new TranslatableText("gui.buildnotes.notes_tab"),
                button -> selectTab(notesTab)
        ));

        this.buildsTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) + 2, 15, tabWidth, tabHeight,
                new TranslatableText("gui.buildnotes.builds_tab"),
                button -> selectTab(buildsTab)
        ));

        // --- LIST WIDGETS ---
        this.noteListWidget = new NoteListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 28);
        this.buildListWidget = new BuildListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 28);

        this.addSelectableChild(this.noteListWidget);
        this.addSelectableChild(this.buildListWidget);


        // --- ACTION BUTTONS ---
        int buttonWidth = 60;
        int buttonHeight = 20;
        int bottomPadding = 10;
        int buttonSpacing = 8;
        int totalButtonWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int buttonsStartX = (this.width - totalButtonWidth) / 2;
        int buttonsY = this.height - buttonHeight - bottomPadding;

        this.addDrawableChild(new ButtonWidget(
                buttonsStartX, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.add_button"),
                button -> System.out.println("Add clicked") // Placeholder
        ));

        this.editButton = this.addDrawableChild(new ButtonWidget(
                buttonsStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.edit_button"),
                button -> System.out.println("Edit clicked") // Placeholder
        ));

        this.deleteButton = this.addDrawableChild(new ButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 2, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.delete_button"),
                button -> System.out.println("Delete clicked") // Placeholder
        ));


        // Set the initial active tab
        selectTab(this.buildsTab);
    }

    private void selectTab(TabButtonWidget selectedTab) {
        boolean isNotes = selectedTab == notesTab;

        notesTab.setActive(isNotes);
        buildsTab.setActive(!isNotes);

        noteListWidget.setVisible(isNotes);
        buildListWidget.setVisible(!isNotes);

        this.noteListWidget.setSelected(null);
        this.buildListWidget.setSelected(null);

        if (isNotes) {
            this.noteListWidget.setNotes(DataManager.getInstance().getNotes());
        } else {
            this.buildListWidget.setBuilds(DataManager.getInstance().getBuilds());
        }

        updateActionButtons();
    }

    public void onNoteSelected() { updateActionButtons(); }

    public void onBuildSelected() { updateActionButtons(); }

    private void updateActionButtons() {
        boolean isNotesActive = this.notesTab.isActive();
        boolean hasSelection = isNotesActive
                ? this.noteListWidget.getSelectedOrNull() != null
                : this.buildListWidget.getSelectedOrNull() != null;

        this.editButton.active = hasSelection;
        this.deleteButton.active = hasSelection;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        if (notesTab.isActive()) {
            this.noteListWidget.render(matrices, mouseX, mouseY, delta);
        } else {
            this.buildListWidget.render(matrices, mouseX, mouseY, delta);
        }

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.keyboard.setRepeatEvents(false);
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}