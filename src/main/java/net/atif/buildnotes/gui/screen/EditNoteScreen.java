package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.data.Scope;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class EditNoteScreen extends BaseScreen {

    private final Note note;

    private MultiLineTextFieldWidget titleField;
    private MultiLineTextFieldWidget contentField;
    private DarkButtonWidget scopeToggleButton;

    public EditNoteScreen(Screen parent, Note note) {
        super(new TranslatableText("gui.buildnotes.edit_note_title"), parent); // A new translation key could be "Editing Note"
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int topMargin = 20;
        int titlePanelHeight = 25;
        int panelSpacing = 5;
        int bottomMargin = 70;

        // --- Title Text Field ---
        this.titleField = new MultiLineTextFieldWidget(
                this.textRenderer,
                contentX,
                topMargin + 5,
                contentWidth,
                titlePanelHeight,
                note.getTitle(),
                "Enter Title Here",
                1, false
        );
        this.addSelectableChild(this.titleField);

        // --- Content Text Field ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        this.contentField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, contentPanelY, contentWidth,
                contentPanelBottom - contentPanelY, note.getContent(),
                "Enter Text Here ", Integer.MAX_VALUE, true
        );
        this.addSelectableChild(this.contentField);

        // --- TOP BUTTON ROW (3) ---
        int topRowY = this.height - (UIHelper.BUTTON_HEIGHT + UIHelper.BOTTOM_PADDING) - UIHelper.BUTTON_HEIGHT - 5;
        List<Text> topButtonTexts = List.of(new LiteralText("Coords"), new LiteralText("Biome"), getScopeButtonText());

        UIHelper.createBottomButtonRow(this, topRowY, topButtonTexts, (index, x, width) -> { // Note the new 'width' parameter
            switch (index) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(0), b -> insertCoords()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(1), b -> insertBiome()));
                case 2 -> {
                    this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topButtonTexts.get(2), b -> {
                        cycleScope();
                        // We now just update the button text, no need to rebuild the whole screen
                        b.setMessage(getScopeButtonText());
                        // Re-init to recalculate button positions and sizes
                        this.init(this.client, this.width, this.height);
                    }));
                }
            }
        });

// --- BOTTOM BUTTON ROW ---
        int bottomRowY = this.height - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;
        List<Text> bottomButtonTexts = List.of(new LiteralText("Save"), new TranslatableText("gui.buildnotes.close_button"));
        UIHelper.createBottomButtonRow(this, bottomRowY, bottomButtonTexts, (index, x, width) -> {
            if (index == 0) {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT, bottomButtonTexts.get(0), button -> saveNote()));
            } else {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT, bottomButtonTexts.get(1), button -> saveAndClose()));
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

    private LiteralText getScopeButtonText() {
        String scopeName;
        Scope currentScope = note.getScope();
        if (currentScope == Scope.GLOBAL) {
            scopeName = "Global";
        } else if (currentScope == Scope.SERVER) {
            scopeName = "Server (Shared)";
        } else {
            // For WORLD scope, the name depends on the context
            scopeName = this.client != null && this.client.isIntegratedServerRunning() ? "World" : "Per-Server";
        }
        return new LiteralText("Scope: " + scopeName);
    }

    private void saveNote() {
        note.setTitle(this.titleField.getText());
        note.setContent(this.contentField.getText());
        note.updateTimestamp();
        DataManager.getInstance().saveNote(this.note);
    }

    private void saveAndClose() {
        saveNote();
        this.close();
    }

    private void insertCoords() {
        if (this.client.player == null) return;
        String coords = String.format("X: %.0f, Y: %.0f, Z: %.0f", this.client.player.getX(), this.client.player.getY(), this.client.player.getZ());
        this.contentField.insertText(coords);
    }

    private void insertBiome() {
        if (this.client.player == null || this.client.world == null) return;
        BlockPos playerPos = this.client.player.getBlockPos();
        String biomeId = this.client.world.getRegistryManager().get(Registry.BIOME_KEY).getId(this.client.world.getBiome(playerPos).value()).toString();
        this.contentField.insertText(biomeId);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int topMargin = 20;
        int titlePanelHeight = 25;
        int panelSpacing = 5;
        int bottomMargin = 70;

        // --- Title Panel ---
        UIHelper.drawPanel(matrices, contentX, topMargin, contentWidth, titlePanelHeight);;
        this.titleField.render(matrices, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        UIHelper.drawPanel(matrices, contentX, contentPanelY, contentWidth, contentPanelBottom - contentPanelY);
        this.contentField.render(matrices, mouseX, mouseY, delta);

        // Draw screen title
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Editing Note"), this.width / 2, 8, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    // We need to override mouseScrolled to pass the event to our custom widget
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.contentField.mouseScrolled(mouseX, mouseY, amount) || super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void close() {
        saveNote();
        super.close();
    }
}