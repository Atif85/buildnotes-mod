package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.template.BuildTemplate;
import net.atif.buildnotes.gui.helper.BuildScreenLayouts;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.List;

public class BuildTemplateScreen extends ScrollableScreen {
    private final EditBuildScreen editBuildScreen;
    private final Build build;
    private List<BuildTemplate> templates = List.of();

    public BuildTemplateScreen(EditBuildScreen parent, Build build) {
        super(Component.translatable("gui.buildnotes.template.title"), parent);
        this.editBuildScreen = parent;
        this.build = build;
    }

    @Override
    protected int getTopMargin() {
        return BuildScreenLayouts.TEMPLATE_TOP_MARGIN;
    }

    @Override
    protected int getBottomMargin() {
        return BuildScreenLayouts.TEMPLATE_BOTTOM_MARGIN;
    }

    @Override
    protected void init() {
        super.init();
        templates = DataManager.getInstance().getAllTemplates();

        List<Component> actions = List.of(
                Component.translatable("gui.buildnotes.template.add_single"),
                Component.translatable("gui.buildnotes.template.save_as")
        );

        int actionsY = BuildScreenLayouts.TEMPLATE_ACTIONS_Y;
        UIHelper.createButtonRow(this, actionsY, actions, (index, x, width) -> {
            if (index == 0) {
                this.addRenderableWidget(new DarkButtonWidget(x, actionsY, width, UIHelper.BUTTON_HEIGHT,
                        actions.get(0), _ -> editBuildScreen.openSingleFieldPrompt()));
            } else {
                DarkButtonWidget saveButton = new DarkButtonWidget(x, actionsY, width, UIHelper.BUTTON_HEIGHT,
                        actions.get(1), _ -> openTemplateNamePrompt());
                saveButton.active = !build.getCustomFields().isEmpty();
                this.addRenderableWidget(saveButton);
            }
        });

        int rowWidth = getTemplateRowWidth();
        int rowX = (this.width - rowWidth) / 2;
        int rowY = 0;

        for (BuildTemplate template : templates) {
            boolean hasDeleteButton = !template.isBuiltIn();
            int deleteWidth = hasDeleteButton ? BuildScreenLayouts.TEMPLATE_DELETE_BUTTON_WIDTH : 0;
            int deleteSpacing = hasDeleteButton ? BuildScreenLayouts.TEMPLATE_DELETE_BUTTON_SPACING : 0;
            int selectionWidth = rowWidth - deleteWidth - deleteSpacing;
            this.addScrollableWidget(new DarkButtonWidget(rowX, rowY, selectionWidth, BuildScreenLayouts.TEMPLATE_ROW_HEIGHT,
                    Component.literal(template.name()), _ -> editBuildScreen.applyTemplate(template)));
            if (hasDeleteButton) {
                this.addScrollableWidget(new DarkButtonWidget(rowX + selectionWidth + deleteSpacing, rowY, deleteWidth, BuildScreenLayouts.TEMPLATE_ROW_HEIGHT,
                        Component.literal("X"), _ -> deleteTemplate(template)));
            }
            rowY += BuildScreenLayouts.TEMPLATE_ROW_HEIGHT + BuildScreenLayouts.TEMPLATE_ROW_SPACING;
        }
        this.totalContentHeight = Math.max(0, rowY - BuildScreenLayouts.TEMPLATE_ROW_SPACING);

        int closeY = UIHelper.getBottomButtonY(this);
        this.addRenderableWidget(new DarkButtonWidget(
                (this.width - UIHelper.BUTTON_WIDTH) / 2, closeY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                Component.translatable("gui.buildnotes.close_button"), _ -> onClose()
        ));
    }

    @Override
    protected void initContent() {
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (templates.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.buildnotes.template.empty"),
                    this.width / 2, BuildScreenLayouts.TEMPLATE_EMPTY_MESSAGE_Y, Colors.TEXT_MUTED);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int top = getTopMargin();
        int bottom = this.height - getBottomMargin();
        graphics.enableScissor(0, top, this.width, bottom);
        Matrix3x2fStack matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.translate(0.0f, (float) (top - this.scrollY));

        int rowWidth = getTemplateRowWidth();
        int rowX = (this.width - rowWidth) / 2;
        int rowY = 0;
        for (BuildTemplate template : templates) {
            if (template.isBuiltIn()) {
                Component tag = Component.translatable("gui.buildnotes.template.builtin_tag");
                graphics.text(this.font, tag, rowX + rowWidth - this.font.width(tag) - BuildScreenLayouts.TEMPLATE_TAG_RIGHT_MARGIN,
                        rowY + (BuildScreenLayouts.TEMPLATE_ROW_HEIGHT - this.font.lineHeight) / 2,
                        Colors.TEMPLATE_BUILT_IN_TAG, false);
            }
            rowY += BuildScreenLayouts.TEMPLATE_ROW_HEIGHT + BuildScreenLayouts.TEMPLATE_ROW_SPACING;
        }
        matrices.popMatrix();
        graphics.disableScissor();
    }

    private int getTemplateRowWidth() {
        return Math.min(this.width - BuildScreenLayouts.TEMPLATE_SCREEN_HORIZONTAL_MARGIN * 2,
                BuildScreenLayouts.TEMPLATE_ROW_MAX_WIDTH);
    }

    private void deleteTemplate(BuildTemplate template) {
        DataManager.getInstance().deleteCustomTemplate(template.id());
        this.open(new BuildTemplateScreen(editBuildScreen, build));
    }

    private void openTemplateNamePrompt() {
        List<String> fieldTitles = build.getCustomFields().stream()
                .map(CustomField::getTitle)
                .toList();
        this.open(new TemplateNameScreen(this, name ->
                DataManager.getInstance().saveCustomTemplate(name, fieldTitles)));
    }

    private static class TemplateNameScreen extends BaseScreen {
        private final java.util.function.Consumer<String> onConfirm;
        private EditBox nameField;
        private DarkButtonWidget saveButton;

        private TemplateNameScreen(Screen parent, java.util.function.Consumer<String> onConfirm) {
            super(Component.translatable("gui.buildnotes.template.prompt_name"), parent);
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            int panelWidth = Math.min(BuildScreenLayouts.TEMPLATE_PROMPT_WIDTH,
                    this.width - BuildScreenLayouts.TEMPLATE_PROMPT_MIN_HORIZONTAL_MARGIN * 2);
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - BuildScreenLayouts.TEMPLATE_PROMPT_HEIGHT) / 2;
            this.nameField = new EditBox(this.font,
                    panelX + BuildScreenLayouts.TEMPLATE_PROMPT_CONTENT_PADDING,
                    panelY + BuildScreenLayouts.TEMPLATE_PROMPT_FIELD_Y,
                    panelWidth - BuildScreenLayouts.TEMPLATE_PROMPT_CONTENT_PADDING * 2,
                    BuildScreenLayouts.TEMPLATE_PROMPT_FIELD_HEIGHT, Component.literal(""));
            this.nameField.setMaxLength(BuildScreenLayouts.TEMPLATE_NAME_MAX_LENGTH);
            this.nameField.setResponder(value -> this.saveButton.active = !value.isBlank());
            this.addWidget(this.nameField);

            List<Component> buttons = List.of(
                    Component.translatable("gui.buildnotes.confirm_button"),
                    Component.translatable("gui.buildnotes.cancel_button")
            );
            int buttonsY = panelY + BuildScreenLayouts.TEMPLATE_PROMPT_BUTTONS_Y;
            UIHelper.createButtonRow(this, buttonsY, buttons, (index, x, width) -> {
                if (index == 0) {
                    this.saveButton = new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT,
                            buttons.get(0), _ -> {
                        String name = this.nameField.getValue().trim();
                        if (!name.isEmpty()) {
                            this.onConfirm.accept(name);
                            this.open(this.parent);
                        }
                    });
                    this.saveButton.active = false;
                    this.addRenderableWidget(this.saveButton);
                } else {
                    this.addRenderableWidget(new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT,
                            buttons.get(1), _ -> this.onClose()));
                }
            });
            this.setInitialFocus(this.nameField);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int panelH = BuildScreenLayouts.TEMPLATE_PROMPT_HEIGHT;
            int panelW = Math.min(BuildScreenLayouts.TEMPLATE_PROMPT_WIDTH,
                    this.width - BuildScreenLayouts.TEMPLATE_PROMPT_MIN_HORIZONTAL_MARGIN * 2);
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            panelH = panelH - (UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2));
            UIHelper.drawPanel(graphics, panelX, panelY, panelW, panelH);
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            graphics.centeredText(this.font, this.title, this.width / 2,
                    panelY + BuildScreenLayouts.TEMPLATE_PROMPT_TITLE_Y, Colors.TEXT_PRIMARY);

            this.nameField.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }
}
