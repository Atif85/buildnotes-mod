package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public class EditNoteScreen extends BaseScreen {

    private final Note note;

    private MultiLineTextFieldWidget titleField;
    private MultiLineTextFieldWidget contentField;

    private DarkButtonWidget globalToggleButton;

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
        UIHelper.createBottomButtonRow(this, topRowY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new LiteralText("Coords"), b -> insertCoords()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new LiteralText("Biome"), b -> insertBiome()));
                case 2 -> {
                    this.globalToggleButton = this.addDrawableChild(new DarkButtonWidget(x, topRowY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                            getGlobalButtonText(), b -> {
                        note.setGlobal(!note.isGlobal());
                        this.globalToggleButton.setMessage(getGlobalButtonText());
                    }));
                }
            }
        });

        // --- BOTTOM BUTTON ROW (2) ---
        int bottomRowY = this.height - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;
        UIHelper.createBottomButtonRow(this, bottomRowY, 2, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 2)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            if (idx == 0) {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new LiteralText("Save"), button -> saveNote()));
            } else {
                this.addDrawableChild(new DarkButtonWidget(x, bottomRowY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        new TranslatableText("gui.buildnotes.close_button"), button -> saveAndClose()));
            }
        });

        this.setInitialFocus(this.titleField);
    }

    private LiteralText getGlobalButtonText() {
        return new LiteralText("Scope: " + (note.isGlobal() ? "Global" : "World"));
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