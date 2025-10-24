package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.*;
import net.atif.buildnotes.gui.TabType;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.TabButtonWidget;
import net.atif.buildnotes.gui.widget.list.BuildListWidget;
import net.atif.buildnotes.gui.widget.list.NoteListWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class MainScreen extends Screen {

    private TabButtonWidget notesTab;
    private TabButtonWidget buildsTab;
    private DarkButtonWidget editButton;
    private DarkButtonWidget deleteButton;
    private DarkButtonWidget addButton;
    private DarkButtonWidget openButton;
    private DarkButtonWidget closeButton;

    private NoteListWidget noteListWidget;
    private BuildListWidget buildListWidget;

    private TabType currentTab;

    public MainScreen(TabType startTab) {
        super(new TranslatableText("gui.buildnotes.main_title"));
        this.currentTab = startTab;
    }

    @Override
    protected void init() {
        super.init();
        this.client.keyboard.setRepeatEvents(true);

        // --- TABS ---
        int tabWidth = 80;
        int tabHeight = 20;
        int topMargin = 60; // Space for title and tabs
        int bottomMargin = 60; // Space for action buttons


        this.notesTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) - tabWidth - 2, 15, tabWidth, tabHeight,
                new TranslatableText("gui.buildnotes.notes_tab"),
                button -> selectTab(TabType.NOTES)
        ));

        this.buildsTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) + 2, 15, tabWidth, tabHeight,
                new TranslatableText("gui.buildnotes.builds_tab"),
                button -> selectTab(TabType.BUILDS)
        ));

        // --- LIST WIDGETS ---
        this.noteListWidget = new NoteListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 28);
        this.buildListWidget = new BuildListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 28);

        this.addSelectableChild(this.noteListWidget);
        this.addSelectableChild(this.buildListWidget);


        // --- ACTION BUTTONS ---
        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomPadding = 12;
        int buttonSpacing = 8;
        int totalButtonWidth = (buttonWidth * 5) + (buttonSpacing * 4);
        int buttonsStartX = (this.width - totalButtonWidth) / 2;
        int buttonsY = this.height - buttonHeight - bottomPadding;

        // Add
        this.addButton = this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.add_button"),
                button -> addEntry()
        ));

        // Open / Select
        this.openButton = this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing), buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.open_button"),
                button -> openSelected()
        ));

        // Edit
        this.editButton = this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 2, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.edit_button"),
                button -> editSelected()
        ));

        // Delete
        this.deleteButton = this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 3, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.delete_button"),
                button -> confirmDelete()
        ));

        // Close / Back
        this.closeButton = this.addDrawableChild(new DarkButtonWidget(
                buttonsStartX + (buttonWidth + buttonSpacing) * 4, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.close_button"),
                button -> this.client.setScreen(null)
        ));

        selectTab(this.currentTab);
    }

    private void selectTab(TabType tab) {
        this.currentTab = tab;
        boolean isNotes = tab == TabType.NOTES;

        notesTab.setActive(isNotes);
        buildsTab.setActive(!isNotes);

        noteListWidget.setVisible(isNotes);
        buildListWidget.setVisible(!isNotes);

        noteListWidget.setSelected(null);
        buildListWidget.setSelected(null);

        if (isNotes) {
            noteListWidget.setNotes(DataManager.getInstance().getNotes());
        } else {
            buildListWidget.setBuilds(DataManager.getInstance().getBuilds());
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

    public void openSelected() {
        if (currentTab == TabType.NOTES) {
            Note sel = noteListWidget.getSelectedNote();
            if (sel != null) {
                this.client.setScreen(new ViewNoteScreen(this, sel));
            }
        } else {
            Build sel = buildListWidget.getSelectedBuild();
            if (sel != null) {
                this.client.setScreen(new ViewBuildScreen(this, sel));
            }
        }
    }

    private void editSelected() {
        if (currentTab == TabType.NOTES) {
            Note sel = noteListWidget.getSelectedNote();
            if (sel != null) {
                this.client.setScreen(new EditNoteScreen(this, sel));
            }
        } else {
            Build sel = buildListWidget.getSelectedBuild();
            if (sel != null) {
                this.client.setScreen(new EditBuildScreen(this, sel));
            }
        }
    }

    private void confirmDelete() {
        DataManager dataManager = DataManager.getInstance();
        Runnable onCancel = () -> { this.client.setScreen(this); };
        
        if (currentTab == TabType.NOTES) {
            Note sel = this.noteListWidget.getSelectedNote();
            if (sel == null) return;

            this.client.setScreen(new ConfirmScreen(this, new LiteralText("Delete note \"" + sel.getTitle() + "\"?"), () -> {
                dataManager.getNotes().removeIf(n -> n.getId().equals(sel.getId()));
                dataManager.saveNotes();
                this.noteListWidget.setNotes(dataManager.getNotes());
                this.client.setScreen(this); // return to main screen
            }, onCancel ));
        } else {
            Build sel = this.buildListWidget.getSelectedBuild();
            if (sel == null) return;

            this.client.setScreen(new ConfirmScreen(this, new LiteralText("Delete build \"" + sel.getName() + "\"?"), () -> {
                dataManager.getBuilds().removeIf(b -> b.getId().equals(sel.getId()));
                dataManager.saveBuilds();
                this.buildListWidget.setBuilds(dataManager.getBuilds());
                this.client.setScreen(this);
            }, onCancel ));
        }
    }


    private void addEntry() {
        if (currentTab == TabType.NOTES) {
            Note newNote = new Note("New Note", ""); // Create a new, empty note
            DataManager.getInstance().getNotes().add(newNote);
            this.client.setScreen(new EditNoteScreen(this, newNote));
        } else {
            Build newBuild = new Build("New Build", "", "", "", ""); // Create a new, empty build
            DataManager.getInstance().getBuilds().add(newBuild); // Add it to the list
            this.client.setScreen(new EditBuildScreen(this, newBuild)); // Open the edit screen immediately
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        if (currentTab == TabType.NOTES) {
            noteListWidget.render(matrices, mouseX, mouseY, delta);
        }
        else {
            buildListWidget.render(matrices, mouseX, mouseY, delta);
        }

        // drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);
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