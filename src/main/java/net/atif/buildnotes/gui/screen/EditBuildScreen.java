package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EditBuildScreen extends ScrollableScreen {

    private final Screen parent;
    private final Build build;

    // We use a map to keep track of which widget corresponds to which data object.
    private MultiLineTextFieldWidget nameField, coordsField, dimensionField, descriptionField, designerField;
    private final Map<CustomField, MultiLineTextFieldWidget> customFieldWidgets = new LinkedHashMap<>();

    private MultiLineTextFieldWidget lastFocusedTextField;
    private DarkButtonWidget globalToggleButton;

    public EditBuildScreen(Screen parent, Build build) {
        super(new TranslatableText("gui.buildnotes.edit_build_title")); // Create a new key e.g., "Editing Build"
        this.parent = parent;
        this.build = build;
    }

    @Override
    protected void init() {
        this.client.keyboard.setRepeatEvents(true);
        super.init(); // This calls initContent()

        // --- FIXED ACTION BUTTONS (Not scrollable) ---
        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomPadding = 12;
        int buttonSpacing = 8;

        // --- Bottom Row ---
        int totalBottomRowWidth = (buttonWidth * 2) + buttonSpacing;
        int bottomRowStartX = (this.width - totalBottomRowWidth) / 2;
        int bottomRowY = this.height - buttonHeight - bottomPadding;

        this.addDrawableChild(new DarkButtonWidget(bottomRowStartX, bottomRowY, buttonWidth, buttonHeight, new LiteralText("Save"), button -> saveBuild()));
        this.addDrawableChild(new DarkButtonWidget(bottomRowStartX + buttonWidth + buttonSpacing, bottomRowY, buttonWidth, buttonHeight, new LiteralText("Back"), button -> saveAndClose()));

        // --- Top Button Row ---
        int totalTopRowWidth = (buttonWidth * 5) + (buttonSpacing * 4);
        int topRowStartX = (this.width - totalTopRowWidth) / 2;
        int topRowY = bottomRowY - buttonHeight - 5;

        this.addDrawableChild(new DarkButtonWidget(topRowStartX, topRowY, buttonWidth, buttonHeight, new LiteralText("Coords"), button -> insertCoords()));
        this.addDrawableChild(new DarkButtonWidget(topRowStartX + (buttonWidth + buttonSpacing), topRowY, buttonWidth, buttonHeight, new LiteralText("Dimension"), button -> insertDimension()));
        this.addDrawableChild(new DarkButtonWidget(topRowStartX + (buttonWidth + buttonSpacing) * 2, topRowY, buttonWidth, buttonHeight, new LiteralText("Biome"), button -> insertBiome()));
        this.addDrawableChild(new DarkButtonWidget(topRowStartX + (buttonWidth + buttonSpacing) * 3, topRowY, buttonWidth, buttonHeight, new LiteralText("Add Field"),
                button -> this.client.setScreen(new RequestFieldTitleScreen(this, this::addCustomField))
        ));

        this.globalToggleButton = this.addDrawableChild(new DarkButtonWidget(topRowStartX + (buttonWidth + buttonSpacing) * 4, topRowY, buttonWidth, buttonHeight, getGlobalButtonText(), button -> {
            build.setGlobal(!build.isGlobal());
            this.globalToggleButton.setMessage(getGlobalButtonText());
        }));

        this.setInitialFocus(this.nameField);
    }

    @Override
    protected void initContent() {
        this.customFieldWidgets.clear();

        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();

        final int panelSpacing = 5;
        final int labelHeight = 12;

        // --- Name Widget ---
        this.nameField = new MultiLineTextFieldWidget(this.textRenderer, contentX, yPos + 5, contentWidth, 25, build.getName(), "Build Name", 1, false);
        this.nameField.setInternalScissoring(false);
        addScrollableWidget(this.nameField);
        yPos += 25 + panelSpacing;

        // --- Coords & Dimension Widgets ---
        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;

        this.coordsField = new MultiLineTextFieldWidget(this.textRenderer, contentX + 50, yPos, fieldWidth - 50, smallFieldHeight, build.getCoordinates(), "X, Y, Z", 1, false);
        this.coordsField.setInternalScissoring(false);
        addScrollableWidget(this.coordsField);

        int dimensionX = contentX + fieldWidth + panelSpacing;
        this.dimensionField = new MultiLineTextFieldWidget(this.textRenderer, dimensionX + 65, yPos, fieldWidth - 65, smallFieldHeight, build.getDimension(), "e.g., Overworld", 1, false);
        this.dimensionField.setInternalScissoring(false);
        addScrollableWidget(this.dimensionField);
        yPos += smallFieldHeight + panelSpacing;

        // --- Description Widget ---
        yPos += labelHeight; // Space for label
        this.descriptionField = new MultiLineTextFieldWidget(this.textRenderer, contentX, yPos, contentWidth, 80, build.getDescription(), "Build Description", Integer.MAX_VALUE, true);
        this.descriptionField.setInternalScissoring(false);
        addScrollableWidget(this.descriptionField);
        yPos += 80 + panelSpacing;

        // --- Designer/Credits Widget ---
        yPos += labelHeight;
        this.designerField = new MultiLineTextFieldWidget(this.textRenderer, contentX, yPos, contentWidth, 40, build.getCredits(), "Designer Credits", Integer.MAX_VALUE, true);
        this.designerField.setInternalScissoring(false);
        addScrollableWidget(this.designerField);
        yPos += 40 + panelSpacing;

        for (CustomField field : this.build.getCustomFields()) {
            yPos += labelHeight;
            int removeBtnWidth = 20;
            int fieldWidgetWidth = contentWidth - removeBtnWidth - panelSpacing;

            MultiLineTextFieldWidget fieldArea = new MultiLineTextFieldWidget(this.textRenderer, contentX, yPos, fieldWidgetWidth, 40, field.getContent(), "", Integer.MAX_VALUE, true);
            fieldArea.setInternalScissoring(false);
            addScrollableWidget(fieldArea);
            this.customFieldWidgets.put(field, fieldArea);

            DarkButtonWidget removeButton = new DarkButtonWidget(contentX + fieldWidgetWidth + panelSpacing, yPos, removeBtnWidth, 20, new LiteralText("X"), button -> removeCustomField(field));
            addScrollableWidget(removeButton);

            yPos += 40 + panelSpacing;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    public void setFocused(Element focused) {
        super.setFocused(focused);
        if (focused instanceof MultiLineTextFieldWidget widget) {
            this.lastFocusedTextField = widget;
        }
    }

    @Override
    protected int getTopMargin() { return 20; }

    @Override
    protected int getBottomMargin() { return 70; } // More space for two rows of buttons

    private void rebuild() {
        saveBuild();
        this.client.setScreen(new EditBuildScreen(this.parent, this.build));
    }

    private LiteralText getGlobalButtonText() {
        return new LiteralText("Scope: " + (build.isGlobal() ? "Global" : "World"));
    }

    @Override
    protected void renderContent(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();

        final int panelSpacing = 5;
        final int labelHeight = 12;

        fill(matrices, contentX, yPos, contentX + contentWidth, yPos + 25, 0x77000000);
        yPos += 25 + panelSpacing;

        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;
        fill(matrices, contentX, yPos, contentX + fieldWidth, yPos + smallFieldHeight, 0x77000000);
        this.textRenderer.draw(matrices, new LiteralText("Coords: ").formatted(Formatting.GRAY), contentX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);

        int dimensionX = contentX + fieldWidth + panelSpacing;
        fill(matrices, dimensionX, yPos, dimensionX + fieldWidth, yPos + smallFieldHeight, 0x77000000);
        this.textRenderer.draw(matrices, new LiteralText("Dimension: ").formatted(Formatting.GRAY), dimensionX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);
        yPos += smallFieldHeight + panelSpacing;

        this.textRenderer.draw(matrices, new LiteralText("Description:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        yPos += labelHeight;
        fill(matrices, contentX, yPos, contentX + contentWidth, yPos + 80, 0x77000000);
        yPos += 80 + panelSpacing;

        this.textRenderer.draw(matrices, new LiteralText("Designer/Credits:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        yPos += labelHeight;
        fill(matrices, contentX, yPos, contentX + contentWidth, yPos + 40, 0x77000000);
        yPos += 40 + panelSpacing;

        for (CustomField field : this.build.getCustomFields()) {
            this.textRenderer.draw(matrices, new LiteralText(field.getTitle() + ":").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
            yPos += labelHeight;
            fill(matrices, contentX, yPos, contentX + contentWidth, yPos + 40, 0x77000000);
            yPos += 40 + panelSpacing;
        }

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
    }

    private void saveBuild() {
        build.setName(nameField.getText());
        build.setCoordinates(coordsField.getText());
        build.setDimension(dimensionField.getText());
        build.setDescription(descriptionField.getText());
        build.setCredits(designerField.getText());

        for (Map.Entry<CustomField, MultiLineTextFieldWidget> entry : customFieldWidgets.entrySet()) {
            entry.getKey().setContent(entry.getValue().getText());
        }
        build.updateTimestamp();
        DataManager.getInstance().saveBuild(this.build);
    }

    private void saveAndClose() {
        saveBuild();
        this.client.setScreen(parent);
    }

    // --- MODIFIED: Renamed and updated logic ---
    private void insertTextAtLastFocus(String text) {
        if (this.lastFocusedTextField != null) {
            this.lastFocusedTextField.insertText(text);
            // After inserting, ensure focus returns to that field
            this.setFocused(this.lastFocusedTextField);
        }
    }

    // --- MODIFIED: These now call the new insert method ---
    private void insertCoords() {
        if (this.client == null || this.client.player == null) return;
        String coords = String.format("%.0f, %.0f, %.0f", this.client.player.getX(), this.client.player.getY(), this.client.player.getZ());
        insertTextAtLastFocus(coords);
    }

    private void insertDimension() {
        if (this.client == null || this.client.player == null) return;
        String dim = this.client.player.world.getRegistryKey().getValue().toString();
        insertTextAtLastFocus(dim);
    }

    private void insertBiome() {
        if (this.client == null || this.client.player == null || this.client.world == null) return;
        BlockPos playerPos = this.client.player.getBlockPos();
        String biomeId = this.client.world.getRegistryManager().get(Registry.BIOME_KEY).getId(this.client.world.getBiome(playerPos).value()).toString();
        insertTextAtLastFocus(biomeId);
    }

    private void addCustomField(String title) {
        if (title == null || title.isBlank()) return;
        this.build.getCustomFields().add(new CustomField(title, ""));
        rebuild();
    }

    private void removeCustomField(CustomField field) {
        this.build.getCustomFields().remove(field);
        rebuild();
    }

    @Override
    public void close() {
        saveBuild();
        this.client.keyboard.setRepeatEvents(false);
        this.client.setScreen(this.parent);
    }

    private static class RequestFieldTitleScreen extends Screen {
        private final Screen parent;
        private final Consumer<String> onConfirm;
        private TextFieldWidget titleField;

        protected RequestFieldTitleScreen(Screen parent, Consumer<String> onConfirm) {
            super(new LiteralText("Enter Field Title"));
            this.parent = parent;
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            int panelW = 200;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - 100) / 2;

            this.titleField = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 20, panelW - 20, 20, new LiteralText(""));
            this.addSelectableChild(this.titleField);

            this.addDrawableChild(new DarkButtonWidget(panelX + 10, panelY + 60, 85, 20, new LiteralText("Confirm"), button -> {
                this.onConfirm.accept(this.titleField.getText());
                this.client.setScreen(this.parent);
            }));
            this.addDrawableChild(new DarkButtonWidget(panelX + 105, panelY + 60, 85, 20, new LiteralText("Cancel"), button -> this.client.setScreen(parent)));
            this.setInitialFocus(this.titleField);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            parent.render(matrices, -1, -1, delta);
            int panelW = 200;
            int panelH = 100;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;
            fill(matrices, panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);
            drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, panelY + 8, 0xFFFFFF);
            this.titleField.render(matrices, mouseX, mouseY, delta);
            super.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }
}