package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.*;
import net.atif.buildnotes.gui.TabType;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.TabButtonWidget;
import net.atif.buildnotes.gui.widget.list.BuildListWidget;
import net.atif.buildnotes.gui.widget.list.NoteListWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.util.List;
import java.util.stream.Collectors;

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
    private MultiLineTextFieldWidget searchField;
    private String searchTerm = "";

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
        int topMargin = 40; // Space for title and tabs
        int bottomMargin = 85; // Space for action buttons


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
        this.noteListWidget = new NoteListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 38);
        this.buildListWidget = new BuildListWidget(this, this.client, this.width, this.height, topMargin, this.height - bottomMargin, 38);

        this.addSelectableChild(this.noteListWidget);
        this.addSelectableChild(this.buildListWidget);

        // --- SEARCH BAR & LABEL ---
        int searchFieldWidth = 160;
        int searchBarHeight = 20;
        int searchFieldX = (this.width - searchFieldWidth) / 2;

        int listBottomY = this.height - bottomMargin;
        int buttonsTopY = this.height - 40;
        int searchBarY = listBottomY + (buttonsTopY - listBottomY - searchBarHeight) / 2;

        this.searchField = new MultiLineTextFieldWidget(this.textRenderer, searchFieldX, searchBarY, searchFieldWidth, searchBarHeight, "", "Search...", 1, false);
        this.searchField.setChangedListener(this::onSearchTermChanged);
        this.addSelectableChild(this.searchField);

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
        this.setInitialFocus(this.searchField);
    }

    private void onSearchTermChanged(String newTerm) {
        this.searchTerm = newTerm.toLowerCase().trim();
        updateLists();
    }

    private void updateLists() {
        if (currentTab == TabType.NOTES) {
            List<Note> allNotes = DataManager.getInstance().getNotes();
            if (!searchTerm.isEmpty()) {
                List<Note> filteredNotes = allNotes.stream()
                        .filter(note -> note.getTitle().toLowerCase().contains(searchTerm))
                        .collect(Collectors.toList());
                noteListWidget.setNotes(filteredNotes);
            } else {
                noteListWidget.setNotes(allNotes);
            }
        } else {
            List<Build> allBuilds = DataManager.getInstance().getBuilds();
            if (!searchTerm.isEmpty()) {
                List<Build> filteredBuilds = allBuilds.stream()
                        .filter(build -> build.getName().toLowerCase().contains(searchTerm))
                        .collect(Collectors.toList());
                buildListWidget.setBuilds(filteredBuilds);
            } else {
                buildListWidget.setBuilds(allBuilds);
            }
        }
        updateActionButtons();
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

        this.searchField.setText("");

        updateLists();

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
                dataManager.deleteNote(sel);
                this.noteListWidget.setNotes(dataManager.getNotes());
                this.client.setScreen(this); // return to main screen
            }, onCancel ));
        } else {
            Build sel = this.buildListWidget.getSelectedBuild();
            if (sel == null) return;

            this.client.setScreen(new ConfirmScreen(this, new LiteralText("Delete build \"" + sel.getName() + "\"?"), () -> {
                dataManager.deleteBuild(sel);
                this.buildListWidget.setBuilds(dataManager.getBuilds());
                this.client.setScreen(this);
            }, onCancel ));
        }
    }

    private void addEntry() {
        if (currentTab == TabType.NOTES) {
            Note newNote = new Note("New Note", ""); // Create a new, empty note
            this.client.setScreen(new EditNoteScreen(this, newNote));
        } else {
            Build newBuild = new Build("New Build", "", "", "", ""); // Create a new, empty build
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

        fill(matrices, this.searchField.x - 2, this.searchField.y, this.searchField.x + this.searchField.width + 2, this.searchField.y + this.searchField.height, 0x77000000);
        this.searchField.render(matrices, mouseX, mouseY, delta);
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