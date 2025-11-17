package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.TabType;
import net.atif.buildnotes.gui.helper.MainScreenLayouts;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.atif.buildnotes.gui.widget.TabButtonWidget;
import net.atif.buildnotes.gui.widget.list.BuildListWidget;
import net.atif.buildnotes.gui.widget.list.NoteListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;
import java.util.stream.Collectors;

public class MainScreen extends BaseScreen {

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
        super(Text.translatable("gui.buildnotes.main_title"), null);
        this.currentTab = startTab;
    }

    protected void init() {
        super.init();

        // --- LAYOUT CALCULATIONS ---
        int buttonsY = UIHelper.getBottomButtonY(this);
        int searchBarY = buttonsY - UIHelper.OUTER_PADDING - MainScreenLayouts.SEARCH_BAR_HEIGHT;
        int bottomMargin = this.height - searchBarY + UIHelper.OUTER_PADDING;

        // Vertically center the tabs in the top margin
        int tabY = (MainScreenLayouts.TOP_MARGIN - MainScreenLayouts.TAB_HEIGHT) / 2;
        this.notesTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) - MainScreenLayouts.TAB_WIDTH - 2, tabY,
                MainScreenLayouts.TAB_WIDTH, MainScreenLayouts.TAB_HEIGHT,
                Text.translatable("gui.buildnotes.notes_tab"), b -> selectTab(TabType.NOTES)
        ));
        this.buildsTab = this.addDrawableChild(new TabButtonWidget(
                (this.width / 2) + 2, tabY,
                MainScreenLayouts.TAB_WIDTH, MainScreenLayouts.TAB_HEIGHT,
                Text.translatable("gui.buildnotes.builds_tab"), b -> selectTab(TabType.BUILDS)
        ));

        // Lists
        this.noteListWidget = new NoteListWidget(this, this.client, MainScreenLayouts.TOP_MARGIN, this.height - bottomMargin, 38);
        this.buildListWidget = new BuildListWidget(this, this.client, MainScreenLayouts.TOP_MARGIN, this.height - bottomMargin, 38);
        this.addSelectableChild(noteListWidget);
        this.addSelectableChild(buildListWidget);

        // Search field
        int searchFieldX = (this.width - MainScreenLayouts.SEARCH_FIELD_WIDTH) / 2;
        this.searchField = new MultiLineTextFieldWidget(
                this.textRenderer, searchFieldX, searchBarY,
                MainScreenLayouts.SEARCH_FIELD_WIDTH, MainScreenLayouts.SEARCH_BAR_HEIGHT,
                "", "Search...", 1, false
        );
        this.searchField.setChangedListener(this::onSearchTermChanged);
        this.addSelectableChild(searchField);

        // Action buttons
        UIHelper.createButtonRow(this, buttonsY, 5, x -> {
            int index = (x - UIHelper.getCenteredButtonStartX(this.width, 5)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (index) {
                case 0 -> this.addButton = this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.add_button"), b -> addEntry()));
                case 1 -> this.openButton = this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.open_button"), b -> openSelected()));
                case 2 -> this.editButton = this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.edit_button"), b -> editSelected()));
                case 3 -> this.deleteButton = this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.delete_button"), b -> confirmDelete()));
                case 4 -> this.closeButton = this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.close_button"), b -> this.client.setScreen(null)));
            }
        });

        refreshData();
        this.setInitialFocus(this.searchField);
    }

    public void refreshData() {
        // This effectively reloads and reapplies the current tab and search filter.
        selectTab(this.currentTab);
    }


    private void onSearchTermChanged(String newTerm) {
        this.searchTerm = newTerm.toLowerCase().trim();
        refreshListContents();
    }

    private void refreshListContents() {
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

        refreshListContents();

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
                open(new ViewNoteScreen(this, sel));
            }
        } else {
            Build sel = buildListWidget.getSelectedBuild();
            if (sel != null) {
                open(new ViewBuildScreen(this, sel));
            }
        }
    }

    private void editSelected() {
        if (currentTab == TabType.NOTES) {
            Note sel = noteListWidget.getSelectedNote();
            if (sel != null) {
                open(new EditNoteScreen(this, sel));
            }
        } else {
            Build sel = buildListWidget.getSelectedBuild();
            if (sel != null) {
                open(new EditBuildScreen(this, sel));
            }
        }
    }

    private void confirmDelete() {
        DataManager dm = DataManager.getInstance();
        if (currentTab == TabType.NOTES) {
            Note sel = noteListWidget.getSelectedNote();
            if (sel != null)
                showConfirm(Text.of("Delete note \"" + sel.getTitle() + "\"?"), () -> {
                    dm.deleteNote(sel);
                    open(this);
                });
        } else {
            Build sel = buildListWidget.getSelectedBuild();
            if (sel != null)
                showConfirm(Text.of("Delete build \"" + sel.getName() + "\"?"), () -> {
                    dm.deleteBuild(sel);
                    open(this);
                });
        }
    }

    private void addEntry() {
        if (currentTab == TabType.NOTES) {
            Note newNote = new Note("New Note", ""); // Create a new, empty note
            open(new EditNoteScreen(this, newNote));
        } else {
            Build newBuild = new Build("New Build", "", "", "", ""); // Create a new, empty build
            open(new EditBuildScreen(this, newBuild)); // Open the edit screen immediately
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        (currentTab == TabType.NOTES ? noteListWidget : buildListWidget).render(context, mouseX, mouseY, delta);

        UIHelper.drawPanel(context, this.searchField.x - 2, this.searchField.y, this.searchField.width + 4, this.searchField.height);

        this.searchField.render(context, mouseX, mouseY, delta);
    }
}