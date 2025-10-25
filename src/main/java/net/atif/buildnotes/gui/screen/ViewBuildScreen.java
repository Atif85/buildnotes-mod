package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.gui.TabType;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class ViewBuildScreen extends ScrollableScreen {

    private final Screen parent;
    private final Build build;

    public ViewBuildScreen(Screen parent, Build build) {
        super(new LiteralText(build.getName()));
        this.parent = parent;
        this.build = build;
    }

    // --- Define scrollable area boundaries ---
    @Override
    protected int getTopMargin() { return 20; }
    @Override
    protected int getBottomMargin() { return 45; }

    @Override
    protected void initContent() {
        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();

        final int panelSpacing = 5;
        final int labelHeight = 12;

        // --- TITLE WIDGET ---
        int titlePanelHeight = 25;
        ReadOnlyMultiLineTextFieldWidget titleArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + 5, contentWidth, titlePanelHeight,
                this.title.getString(), 1, false
        );
        titleArea.setInternalScissoring(false);
        addScrollableWidget(titleArea);
        yPos += titlePanelHeight + panelSpacing;

        // --- COORDS & DIMENSION WIDGETS ---
        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;

        // Coords Widget (positioned after the label)
        int coordsTextX = contentX + 50;
        ReadOnlyMultiLineTextFieldWidget coordsArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, coordsTextX, yPos, fieldWidth - 50, smallFieldHeight,
                build.getCoordinates(), 1, false
        );
        coordsArea.setInternalScissoring(false);
        addScrollableWidget(coordsArea);

        // Dimension Widget (positioned after the label)
        int dimensionX = contentX + fieldWidth + panelSpacing;
        int dimensionTextX = dimensionX + 65;
        ReadOnlyMultiLineTextFieldWidget dimensionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, dimensionTextX, yPos, fieldWidth - 65, smallFieldHeight,
                build.getDimension(), 1, false
        );
        dimensionArea.setInternalScissoring(false);
        addScrollableWidget(dimensionArea);
        yPos += smallFieldHeight + panelSpacing;

        // --- DESCRIPTION WIDGET ---
        int descriptionHeight = 80;
        ReadOnlyMultiLineTextFieldWidget descriptionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + labelHeight, contentWidth, descriptionHeight,
                build.getDescription(), Integer.MAX_VALUE, true
        );
        descriptionArea.setInternalScissoring(false);
        addScrollableWidget(descriptionArea);
        yPos += descriptionHeight + labelHeight + panelSpacing;

        // --- CREDITS WIDGET ---
        int creditsHeight = 40;
        ReadOnlyMultiLineTextFieldWidget creditsArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + labelHeight, contentWidth, creditsHeight,
                build.getCredits(), Integer.MAX_VALUE, true
        );
        creditsArea.setInternalScissoring(false);
        addScrollableWidget(creditsArea);
        yPos += creditsHeight + labelHeight + panelSpacing;

        // --- CUSTOM FIELD WIDGETS ---
        for (CustomField field : build.getCustomFields()) {
            int fieldHeight = 40;
            ReadOnlyMultiLineTextFieldWidget fieldArea = new ReadOnlyMultiLineTextFieldWidget(
                    this.textRenderer, contentX, yPos + labelHeight, contentWidth, fieldHeight,
                    field.getContent(), Integer.MAX_VALUE, true
            );
            fieldArea.setInternalScissoring(false);
            addScrollableWidget(fieldArea);
            yPos += fieldHeight + labelHeight + panelSpacing;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    protected void init() {
        super.init(); // This will call initContent() from the parent class

        // These buttons are fixed and not part of the scrollable content
        int buttonWidth = 80;
        int buttonHeight = 20;
        int bottomPadding = 12;
        int buttonSpacing = 8;
        int totalButtonWidth = (buttonWidth * 3) + (buttonSpacing * 2);
        int buttonsStartX = (this.width - totalButtonWidth) / 2;
        int buttonsY = this.height - buttonHeight - bottomPadding;

        this.addDrawableChild(new DarkButtonWidget(buttonsStartX, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.close_button"), button -> this.client.setScreen(parent)));

        this.addDrawableChild(new DarkButtonWidget(buttonsStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.edit_button"), button -> this.client.setScreen(new EditBuildScreen(this.parent, this.build)))
        );

        this.addDrawableChild(new DarkButtonWidget(buttonsStartX + (buttonWidth + buttonSpacing) * 2, buttonsY, buttonWidth, buttonHeight,
                new TranslatableText("gui.buildnotes.delete_button"),button -> confirmDelete()));
    }

    @Override
    protected void renderContent(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();

        final int panelSpacing = 5;
        final int labelHeight = 12;

        // --- TITLE ---
        int titlePanelHeight = 25;
        fill(matrices, contentX, yPos, contentX + contentWidth, yPos + titlePanelHeight, 0x77000000);
        yPos += titlePanelHeight + panelSpacing;

        // --- COORDS & DIMENSION ---
        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;

        // Backgrounds and Labels only
        fill(matrices, contentX, yPos, contentX + fieldWidth, yPos + smallFieldHeight, 0x77000000);
        this.textRenderer.draw(matrices, new LiteralText("Coords: ").formatted(Formatting.GRAY), contentX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);

        int dimensionX = contentX + fieldWidth + panelSpacing;
        fill(matrices, dimensionX, yPos, dimensionX + fieldWidth, yPos + smallFieldHeight, 0x77000000);
        this.textRenderer.draw(matrices, new LiteralText("Dimension: ").formatted(Formatting.GRAY), dimensionX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);
        yPos += smallFieldHeight + panelSpacing;

        // --- DYNAMIC CONTENT ---
        int descriptionHeight = 80;
        this.textRenderer.draw(matrices, new LiteralText("Description:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        fill(matrices, contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + descriptionHeight, 0x77000000);
        yPos += descriptionHeight + labelHeight + panelSpacing;

        int creditsHeight = 40;
        this.textRenderer.draw(matrices, new LiteralText("Credits:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        fill(matrices, contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + creditsHeight, 0x77000000);
        yPos += creditsHeight + labelHeight + panelSpacing;

        for (CustomField field : build.getCustomFields()) {
            int fieldHeight = 40;
            this.textRenderer.draw(matrices, new LiteralText(field.getTitle() + ":").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
            fill(matrices, contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + fieldHeight, 0x77000000);
            yPos += fieldHeight + labelHeight + panelSpacing;
        }
    }

    private void confirmDelete() {
        Runnable onConfirm = () -> {
            DataManager.getInstance().deleteBuild(this.build);
            this.client.setScreen(new MainScreen(TabType.BUILDS));
        };
        Runnable onCancel = () -> this.client.setScreen(this);
        this.client.setScreen(new ConfirmScreen(this, new LiteralText("Delete build \"" + build.getName() + "\"?"), onConfirm, onCancel));
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}