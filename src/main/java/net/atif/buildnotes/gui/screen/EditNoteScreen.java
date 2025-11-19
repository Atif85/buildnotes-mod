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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.List;

public class EditNoteScreen extends BaseScreen {

    private final Note note;

    private MultiLineTextFieldWidget titleField;
    private MultiLineTextFieldWidget contentField;
    private MultiLineTextFieldWidget lastFocusedTextField;

    public EditNoteScreen(Screen parent, Note note) {
        super(Text.translatable("gui.buildnotes.edit_note_title"), parent);
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginDoubleRow();

        // --- Title Text Field ---
        this.titleField = new MultiLineTextFieldWidget(
                this.textRenderer,
                contentX,
                NoteScreenLayouts.TOP_MARGIN + 5,
                contentWidth,
                NoteScreenLayouts.TITLE_PANEL_HEIGHT,
                note.getTitle(),
                Text.translatable("gui.buildnotes.placeholder.title").getString(),
                1, false
        );
        this.addSelectableChild(this.titleField);

        // --- Content Text Field ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
        int contentPanelBottom = this.height - bottomMargin;
        this.contentField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, contentPanelY, contentWidth,
                contentPanelBottom - contentPanelY, note.getContent(),
                Text.translatable("gui.buildnotes.placeholder.note_content").getString(), Integer.MAX_VALUE, true
        );
        this.addSelectableChild(this.contentField);

        // --- TOP BUTTON ROW (3) ---
        int topRowY = UIHelper.getBottomButtonY(this, 1);
        List<Text> topButtonTexts = List.of(
                Text.translatable("gui.buildnotes.edit.coords"),
                Text.translatable("gui.buildnotes.edit.biome"),
                getScopeButtonText()
        );

        UIHelper.createButtonRow(this, topRowY, topButtonTexts, (index, x, width) -> { // Note the new 'width' parameter
            switch (index) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(0), b -> insertCoords()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(1), b -> insertBiome()));
                case 2 ->
                        this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(2), b -> {
                            saveNote();
                            cycleScope();
                            this.init(this.client, this.width, this.height);
                        }));
            }
        });

        // --- BOTTOM BUTTON ROW ---
        int bottomRowY = UIHelper.getBottomButtonY(this, 2);
        List<Text> bottomButtonTexts = List.of(
                Text.translatable("gui.buildnotes.save_button"),
                Text.translatable("gui.buildnotes.close_button")
        );
        UIHelper.createButtonRow(this, bottomRowY, bottomButtonTexts, (index, x, width) -> {
            if (index == 0) {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT,
                    bottomButtonTexts.get(0), button -> {
                        saveNote();
                        open(new ViewNoteScreen(this.parent, this.note));
                    })
                );
            } else {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT, bottomButtonTexts.get(1), button -> this.close()));
            }
        });

        this.setInitialFocus(this.titleField);
    }

    @Override
    public void tick() {
        super.tick();
        // Keep track of the last text field that had focus
        if (this.titleField.isFocused()) {
            this.lastFocusedTextField = this.titleField;
        } else if (this.contentField.isFocused()) {
            this.lastFocusedTextField = this.contentField;
        }
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

    private Text getScopeButtonText() {
        Text scopeName;
        Scope currentScope = note.getScope();
        if (currentScope == Scope.GLOBAL) {
            scopeName = Text.translatable("gui.buildnotes.edit.scope.global");
        } else if (currentScope == Scope.SERVER) {
            scopeName = Text.translatable("gui.buildnotes.edit.scope.server");
        } else {
            scopeName = this.client != null && this.client.isIntegratedServerRunning()
                    ? Text.translatable("gui.buildnotes.edit.scope.world")
                    : Text.translatable("gui.buildnotes.edit.scope.per_server");
        }
        return Text.translatable("gui.buildnotes.edit.scope_button", scopeName);
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
        if (this.client.player == null) return;
        String coords = String.format("X: %.0f, Y: %.0f, Z: %.0f", this.client.player.getX(), this.client.player.getY(), this.client.player.getZ());
        insertTextAtLastFocus(coords);
    }

    private void insertBiome() {
        if (this.client.player == null || this.client.world == null) return;
        BlockPos playerPos = this.client.player.getBlockPos();
        RegistryEntry<Biome> biomeEntry = this.client.world.getBiome(playerPos);
        String biomeId = biomeEntry.getKey().map(RegistryKey::getValue).map(Identifier::toString).orElse("minecraft:unknown");
        insertTextAtLastFocus(biomeId);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int contentWidth = (int) (this.width * NoteScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int bottomMargin = NoteScreenLayouts.getBottomMarginDoubleRow();

        // --- Title Panel ---
        UIHelper.drawPanel(context, contentX, NoteScreenLayouts.TOP_MARGIN, contentWidth, NoteScreenLayouts.TITLE_PANEL_HEIGHT);
        this.titleField.render(context, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = NoteScreenLayouts.TOP_MARGIN + NoteScreenLayouts.TITLE_PANEL_HEIGHT + NoteScreenLayouts.PANEL_SPACING;
        int contentPanelBottom = this.height - bottomMargin;
        UIHelper.drawPanel(context, contentX, contentPanelY, contentWidth, contentPanelBottom - contentPanelY);
        this.contentField.render(context, mouseX, mouseY, delta);

        // Draw screen title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, Colors.TEXT_PRIMARY);
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
    public void close() {
        saveNote();
        super.close();
    }
}