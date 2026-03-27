package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.Scope;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.NoteScreenLayouts;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;

public class EditNoteScreen extends BaseScreen {

    private final Note note;

    private MultiLineTextFieldWidget titleField;
    private MultiLineTextFieldWidget contentField;
    private MultiLineTextFieldWidget lastFocusedTextField;

    public EditNoteScreen(Screen parent, Note note) {
        super(Component.translatable("gui.buildnotes.edit_note_title"), parent);
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginDoubleRow();

        // --- Title Component Field ---
        this.titleField = new MultiLineTextFieldWidget(
                this.font,
                contentX,
                NoteScreenLayouts.TOP_MARGIN + 5,
                contentWidth,
                NoteScreenLayouts.TITLE_PANEL_HEIGHT,
                note.getTitle(),
                Component.translatable("gui.buildnotes.placeholder.title").getString(),
                1, false
        );
        this.addWidget(this.titleField);

        // --- Content Component Field ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
        int contentPanelBottom = this.height - bottomMargin;
        this.contentField = new MultiLineTextFieldWidget(
                this.font, contentX, contentPanelY, contentWidth,
                contentPanelBottom - contentPanelY, note.getContent(),
                Component.translatable("gui.buildnotes.placeholder.note_content").getString(), Integer.MAX_VALUE, true
        );
        this.addWidget(this.contentField);

        // --- TOP BUTTON ROW (3) ---
        int topRowY = UIHelper.getBottomButtonY(this, 1);
        List<Component> topButtonTexts = List.of(
                Component.translatable("gui.buildnotes.edit.coords"),
                Component.translatable("gui.buildnotes.edit.biome"),
                getScopeButtonText()
        );

        UIHelper.createButtonRow(this, topRowY, topButtonTexts, (index, x, width) -> { // Note the new 'width' parameter
            switch (index) {
                case 0 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(0), b -> insertCoords()));
                case 1 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(1), b -> insertBiome()));
                case 2 ->
                        this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(2), b -> {
                            saveNote();
                            cycleScope();
                            this.init(this.width, this.height);
                        }));
            }
        });

        // --- BOTTOM BUTTON ROW ---
        int bottomRowY = UIHelper.getBottomButtonY(this, 2);
        List<Component> bottomButtonTexts = List.of(
                Component.translatable("gui.buildnotes.save_button"),
                Component.translatable("gui.buildnotes.close_button")
        );
        UIHelper.createButtonRow(this, bottomRowY, bottomButtonTexts, (index, x, width) -> {
            if (index == 0) {
                this.addRenderableWidget(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT,
                    bottomButtonTexts.get(0), button -> {
                        saveNote();
                        open(new ViewNoteScreen(this.parent, this.note));
                    })
                );
            } else {
                this.addRenderableWidget(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT, bottomButtonTexts.get(1), button -> this.onClose()));
            }
        });

        this.setInitialFocus(this.titleField);
    }

    private void cycleScope() {
        Scope currentScope = note.getScope();
        if (ClientSession.isOnServer() && ClientSession.hasEditPermission()) {
            // Cycle through all three: WORLD -> GLOBAL -> SERVER -> WORLD
            if (currentScope == Scope.WORLD) note.setScope(Scope.GLOBAL);
            else if (currentScope == Scope.GLOBAL) note.setScope(Scope.SERVER);
            else note.setScope(Scope.WORLD);
        } else {
            // Cycle between WORLD and GLOBAL
            note.setScope(currentScope == Scope.WORLD ? Scope.GLOBAL : Scope.WORLD);
        }
    }

    private Component getScopeButtonText() {
        Component scopeName;
        Scope currentScope = note.getScope();
        if (currentScope == Scope.GLOBAL) {
            scopeName = Component.translatable("gui.buildnotes.edit.scope.global");
        } else if (currentScope == Scope.SERVER) {
            scopeName = Component.translatable("gui.buildnotes.edit.scope.server");
        } else {
            scopeName = this.minecraft.isSingleplayer()
                    ? Component.translatable("gui.buildnotes.edit.scope.world")
                    : Component.translatable("gui.buildnotes.edit.scope.per_server");
        }
        return Component.translatable("gui.buildnotes.edit.scope_button", scopeName);
    }

    private void saveNote() {
        note.setTitle(this.titleField.getText());
        note.setContent(this.contentField.getText());
        note.updateTimestamp();
        DataManager.getInstance().saveNote(this.note);
    }

    private void insertTextAtLastFocus(String text) {
        if (this.lastFocusedTextField != null) {
            this.lastFocusedTextField.insertText(text);
            this.setFocused(this.lastFocusedTextField);
        } else if (this.contentField != null) { // Fallback to the content field
            this.contentField.insertText(text);
            this.setFocused(this.contentField);
        }
    }

    private void insertCoords() {
        if (this.minecraft.player == null) return;
        String coords = String.format("X: %.0f, Y: %.0f, Z: %.0f", this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ());
        insertTextAtLastFocus(coords);
    }

    private void insertBiome() {
        if (this.minecraft.player == null || this.minecraft.level == null) return;

        BlockPos playerPos = this.minecraft.player.blockPosition();
        Holder<net.minecraft.world.level.biome.Biome> biomeEntry = this.minecraft.level.getBiome(playerPos);

        String biomeId = biomeEntry.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Identifier::toString)
                .orElse("minecraft:unknown");
        insertTextAtLastFocus(biomeId);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginDoubleRow();

        // --- Title Panel ---
        UIHelper.drawPanel(context, contentX, NoteScreenLayouts.TOP_MARGIN, contentWidth, NoteScreenLayouts.TITLE_PANEL_HEIGHT);
        this.titleField.extractRenderState(context, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
        int contentPanelBottom = this.height - bottomMargin;
        UIHelper.drawPanel(context, contentX, contentPanelY, contentWidth, contentPanelBottom - contentPanelY);
        this.contentField.extractRenderState(context, mouseX, mouseY, delta);

        // Draw screen title
        context.centeredText(this.font, this.title, this.width / 2, 8, Colors.TEXT_PRIMARY);
    }

    // We need to override mouseScrolled to pass the event to our custom widget
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.contentField.isMouseOver(mouseX, mouseY)) {
            return this.contentField.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (this.titleField.isMouseOver(mouseX, mouseY)) {
            return this.titleField.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof MultiLineTextFieldWidget widget) {
            this.lastFocusedTextField = widget;
        }
    }

    @Override
    public void onClose() {
        saveNote();
        super.onClose();
    }
}