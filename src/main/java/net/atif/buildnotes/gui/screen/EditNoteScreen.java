package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Note;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.atif.buildnotes.gui.widget.TransparentTextFieldWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public class EditNoteScreen extends Screen {

    private final Screen parent;
    private final Note note;

    private MultiLineTextFieldWidget titleField;
    private MultiLineTextFieldWidget contentField;

    public EditNoteScreen(Screen parent, Note note) {
        super(new TranslatableText("gui.buildnotes.edit_note_title")); // A new translation key could be "Editing Note"
        this.parent = parent;
        this.note = note;
    }

    @Override
    protected void init() {
        super.init();
        this.client.keyboard.setRepeatEvents(true);

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int topMargin = 20;
        int titlePanelHeight = 25;
        int panelSpacing = 5;
        int bottomMargin = 70; // More space for two rows of buttons

        // --- Title Text Field ---
        this.titleField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, topMargin + 5, contentWidth,
                titlePanelHeight, note.getTitle(), 1, false
        );
        this.addSelectableChild(this.titleField);

        // --- Content Text Field ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        this.contentField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, contentPanelY, contentWidth,
                contentPanelBottom - contentPanelY, note.getContent(),
                Integer.MAX_VALUE, true
        );
        this.addSelectableChild(this.contentField);

        // --- ACTION BUTTONS ---
        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomPadding = 12;
        int buttonSpacing = 8;

        // Bottom Row
        int totalBottomRowWidth = (buttonWidth * 2) + buttonSpacing;
        int bottomRowStartX = (this.width - totalBottomRowWidth) / 2;
        int bottomRowY = this.height - buttonHeight - bottomPadding;

        this.addDrawableChild(new DarkButtonWidget(bottomRowStartX, bottomRowY, buttonWidth, buttonHeight, new LiteralText("Save"), button -> saveAndClose()));
        this.addDrawableChild(new DarkButtonWidget(bottomRowStartX + buttonWidth + buttonSpacing, bottomRowY, buttonWidth, buttonHeight, new LiteralText("Back"), button -> saveAndClose()));

        // Top Button Row
        int totalTopRowWidth = (buttonWidth * 2) + buttonSpacing;
        int topRowStartX = (this.width - totalTopRowWidth) / 2;
        int topRowY = bottomRowY - buttonHeight - 5;

        this.addDrawableChild(new DarkButtonWidget(topRowStartX, topRowY, buttonWidth, buttonHeight, new LiteralText("Coords"), button -> insertCoords()));
        this.addDrawableChild(new DarkButtonWidget(topRowStartX + buttonWidth + buttonSpacing, topRowY, buttonWidth, buttonHeight, new LiteralText("Biome"), button -> insertBiome()));

        this.setInitialFocus(this.titleField);
    }

    private void saveNote() {
        // Update the note object with the new text from the widgets
        note.setTitle(this.titleField.getText());
        note.setContent(this.contentField.getText());
        note.updateTimestamp();
        DataManager.getInstance().saveNotes();
    }

    private void saveAndClose() {
        saveNote();
        this.client.setScreen(this.parent);
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
        this.contentField.insertText("[" + biomeId + "]");
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
        fill(matrices, contentX, topMargin, contentX + contentWidth, topMargin + titlePanelHeight, 0x77000000);
        this.titleField.render(matrices, mouseX, mouseY, delta);

        // --- Content Panel ---
        int contentPanelY = topMargin + titlePanelHeight + panelSpacing;
        int contentPanelBottom = this.height - bottomMargin;
        fill(matrices, contentX, contentPanelY, contentX + contentWidth, contentPanelBottom, 0x77000000);
        // Manually render our custom widget
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
        // Ensure the screen is closed properly
        this.client.keyboard.setRepeatEvents(false);
        this.client.setScreen(this.parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}