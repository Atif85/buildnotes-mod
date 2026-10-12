package net.atif.buildnotes.gui.screen;

import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.template.BuildTemplate;
import net.atif.buildnotes.gui.helper.BuildScreenLayouts;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.LiteralText;

import java.util.List;

public class BuildTemplateScreen extends ScrollableScreen {
    private final EditBuildScreen editBuildScreen;
    private final Build build;
    private List<BuildTemplate> templates = List.of();

    public BuildTemplateScreen(EditBuildScreen parent, Build build) {
        super(new TranslatableText("gui.buildnotes.template.title"), parent);
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

        List<Text> actions = List.of(
                new TranslatableText("gui.buildnotes.template.add_single"),
                new TranslatableText("gui.buildnotes.template.save_as")
        );

        int actionsY = BuildScreenLayouts.TEMPLATE_ACTIONS_Y;
        UIHelper.createButtonRow(this, actionsY, actions, (index, x, width) -> {
            if (index == 0) {
                this.addDrawableChild(new DarkButtonWidget(x, actionsY, width, UIHelper.BUTTON_HEIGHT,
                        actions.get(0), button -> editBuildScreen.openSingleFieldPrompt()));
            } else {
                DarkButtonWidget saveButton = new DarkButtonWidget(x, actionsY, width, UIHelper.BUTTON_HEIGHT,
                        actions.get(1), button -> openTemplateNamePrompt());
                saveButton.active = !build.getCustomFields().isEmpty();
                this.addDrawableChild(saveButton);
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
                    new LiteralText(template.name()), button -> editBuildScreen.applyTemplate(template)));

            if (hasDeleteButton) {
                this.addScrollableWidget(new DarkButtonWidget(rowX + selectionWidth + deleteSpacing, rowY, deleteWidth, BuildScreenLayouts.TEMPLATE_ROW_HEIGHT,
                        new LiteralText("X"), button -> deleteTemplate(template)));
            }
            rowY += BuildScreenLayouts.TEMPLATE_ROW_HEIGHT + BuildScreenLayouts.TEMPLATE_ROW_SPACING;
        }

        this.totalContentHeight = Math.max(0, rowY - BuildScreenLayouts.TEMPLATE_ROW_SPACING);

        int closeY = UIHelper.getBottomButtonY(this);
        this.addDrawableChild(new DarkButtonWidget(
                (this.width - UIHelper.BUTTON_WIDTH) / 2, closeY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                new TranslatableText("gui.buildnotes.close_button"), button -> this.close()
        ));
    }

    @Override
    protected void initContent() {
    }

    @Override
    protected void renderContent(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (templates.isEmpty()) {
            client.textRenderer.drawWithShadow(matrices, new TranslatableText("gui.buildnotes.template.empty"),
                    (float) this.width / 2, BuildScreenLayouts.TEMPLATE_EMPTY_MESSAGE_Y, Colors.TEXT_MUTED);
            return;
        }
    }

    @Override
    protected void renderForeground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int rowWidth = getTemplateRowWidth();
        int rowX = (this.width - rowWidth) / 2;
        int rowY = 0;

        for (BuildTemplate template : templates) {
            if (template.isBuiltIn()) {
                Text tag = new TranslatableText("gui.buildnotes.template.builtin_tag");
                int tagX = rowX + rowWidth - this.textRenderer.getWidth(tag) - BuildScreenLayouts.TEMPLATE_TAG_RIGHT_MARGIN;
                int tagY = rowY + (BuildScreenLayouts.TEMPLATE_ROW_HEIGHT - this.textRenderer.fontHeight) / 2;

                client.textRenderer.drawWithShadow(matrices, tag, tagX, tagY, Colors.TEMPLATE_BUILT_IN_TAG);
            }
            rowY += BuildScreenLayouts.TEMPLATE_ROW_HEIGHT + BuildScreenLayouts.TEMPLATE_ROW_SPACING;
        }
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
        private TextFieldWidget titleField;
        private DarkButtonWidget saveButton;

        private TemplateNameScreen(Screen parent, java.util.function.Consumer<String> onConfirm) {
            super(new TranslatableText("gui.buildnotes.template.prompt_name"), parent);
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            int panelWidth = Math.min(BuildScreenLayouts.TEMPLATE_PROMPT_WIDTH,
                    this.width - BuildScreenLayouts.TEMPLATE_PROMPT_MIN_HORIZONTAL_MARGIN * 2);
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - BuildScreenLayouts.TEMPLATE_PROMPT_HEIGHT) / 2;

            this.titleField = new TextFieldWidget (this.textRenderer,
                    panelX + BuildScreenLayouts.TEMPLATE_PROMPT_CONTENT_PADDING,
                    panelY + BuildScreenLayouts.TEMPLATE_PROMPT_FIELD_Y,
                    panelWidth - BuildScreenLayouts.TEMPLATE_PROMPT_CONTENT_PADDING * 2,
                    BuildScreenLayouts.TEMPLATE_PROMPT_FIELD_HEIGHT, new LiteralText(""));
            this.titleField.setMaxLength(BuildScreenLayouts.TEMPLATE_NAME_MAX_LENGTH);
            this.titleField.setChangedListener(value -> this.saveButton.active = !value.isBlank());
            this.addSelectableChild(this.titleField);

            List<Text> buttons = List.of(
                    new TranslatableText("gui.buildnotes.confirm_button"),
                    new TranslatableText("gui.buildnotes.cancel_button")
            );
            int buttonsY = panelY + BuildScreenLayouts.TEMPLATE_PROMPT_BUTTONS_Y;
            UIHelper.createButtonRow(this, buttonsY, buttons, (index, x, width) -> {
                if (index == 0) {
                    this.saveButton = new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT,
                            buttons.get(0), button -> {
                        String name = this.titleField.getText().trim();
                        if (!name.isEmpty()) {
                            this.onConfirm.accept(name);
                            this.open(this.parent);
                        }
                    });
                    this.saveButton.active = false;
                    this.addDrawableChild(this.saveButton);
                } else {
                    this.addDrawableChild(new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT,
                            buttons.get(1), button -> this.close()));
                }
            });
            this.setInitialFocus(this.titleField);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);
            int panelH = BuildScreenLayouts.TEMPLATE_PROMPT_HEIGHT;
            int panelW = Math.min(BuildScreenLayouts.TEMPLATE_PROMPT_WIDTH,
                    this.width - BuildScreenLayouts.TEMPLATE_PROMPT_MIN_HORIZONTAL_MARGIN * 2);

            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            panelH = panelH - (UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2));
            UIHelper.drawPanel(matrices, panelX, panelY, panelW, panelH);

            DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2,
                    panelY + BuildScreenLayouts.TEMPLATE_PROMPT_TITLE_Y, Colors.TEXT_PRIMARY);

            this.titleField.render(matrices, mouseX, mouseY, delta);
        }
    }
}
