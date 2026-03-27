package net.atif.buildnotes.gui.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.client.ClientImageTransferManager;
import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Scope;
import net.atif.buildnotes.gui.helper.BuildScreenLayouts;
import net.atif.buildnotes.gui.helper.Colors;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

public class EditBuildScreen extends ScrollableScreen {

    private final Build build;

    // --- Image Gallery Fields ---
    private record ImageData(Identifier textureId, int width, int height) {}
    private int currentImageIndex = 0;
    private final Map<String, ImageData> textureCache = new HashMap<>();
    private final Set<String> downloadingImages = new HashSet<>();

    private DarkButtonWidget prevImageButton;
    private DarkButtonWidget nextImageButton;

    // --- Standard Fields ---
    private MultiLineTextFieldWidget nameField, coordsField, dimensionField, descriptionField, designerField;
    private final Map<CustomField, MultiLineTextFieldWidget> customFieldWidgets = new LinkedHashMap<>();
    private MultiLineTextFieldWidget lastFocusedTextField;

    public EditBuildScreen(Screen parent, Build build) {
        super(Component.translatable("gui.buildnotes.edit_build_title"), parent);
        this.build = build;
    }

    @Override
    protected int getTopMargin() { return 20; }

    @Override
    protected int getBottomMargin() {
        return (UIHelper.BUTTON_HEIGHT * 2) + (UIHelper.OUTER_PADDING * 2) + UIHelper.BUTTON_ROW_SPACING;
    }

    @Override
    protected void init() {
        super.init(); // calls initContent()

        // --- BOTTOM BUTTON ROW ---
        int bottomRowY = UIHelper.getBottomButtonY(this, 2);
        List<Component> bottomTexts = List.of(
                Component.translatable("gui.buildnotes.save_button"),
                Component.translatable("gui.buildnotes.close_button")
        );
        UIHelper.createButtonRow(this, bottomRowY, bottomTexts, (index, x, width) -> {
            if (index == 0) {
                this.addRenderableWidget(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT,
                    bottomTexts.get(0), button -> {
                        saveBuild();
                        open(new ViewBuildScreen(this.parent, this.build));
                    })
                );
            } else {
                this.addRenderableWidget(new DarkButtonWidget(x, bottomRowY, width, UIHelper.BUTTON_HEIGHT, bottomTexts.get(1), button -> this.onClose()));
            }
        });

        // --- TOP BUTTON ROW ---
        int topRowY = UIHelper.getBottomButtonY(this, 1);
        List<Component> topTexts = List.of(
                Component.translatable("gui.buildnotes.edit.coords"),
                Component.translatable("gui.buildnotes.edit.dimension"),
                Component.translatable("gui.buildnotes.edit.biome"),
                Component.translatable("gui.buildnotes.edit.add_images"),
                Component.translatable("gui.buildnotes.edit.add_field"),
                getScopeButtonText()
        );
        UIHelper.createButtonRow(this, topRowY, topTexts, (index, x, width) -> {
            switch (index) {
                case 0 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(0), b -> insertCoords()));
                case 1 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(1), b -> insertDimension()));
                case 2 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(2), b -> insertBiome()));
                case 3 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(3), b -> openImageSelectionDialog()));
                case 4 -> this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(4), b -> {
                    saveBuild();
                    this.open(new RequestFieldTitleScreen(this, this::addCustomField));
                }));
                case 5 ->
                        this.addRenderableWidget(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(5), button -> {
                            saveBuild();
                            cycleScope();
                            this.init(this.width, this.height);
                        }));
            }
        });

        // --- IMAGE GALLERY WIDGETS ---
        if (!build.getImageFileNames().isEmpty()) {
            int panelSpacing = BuildScreenLayouts.PANEL_SPACING;

            int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
            int contentX = (this.width - contentWidth) / 2;
            int galleryHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
            int galleryY = getTopMargin() + BuildScreenLayouts.NAME_FIELD_HEIGHT + panelSpacing + BuildScreenLayouts.SMALL_FIELD_HEIGHT + panelSpacing;
            int navButtonY = galleryY + (galleryHeight - 20) / 2;

            prevImageButton = new DarkButtonWidget(contentX - 25, navButtonY, 20, 20, Component.translatable("gui.buildnotes.gallery.previous"), b -> switchImage(-1));
            nextImageButton = new DarkButtonWidget(contentX + contentWidth + 5, navButtonY, 20, 20, Component.translatable("gui.buildnotes.gallery.next"), b -> switchImage(1));
            DarkButtonWidget deleteImageButton = new DarkButtonWidget(contentX + contentWidth - 22, galleryY + 2, 20, 20, Component.translatable("gui.buildnotes.gallery.delete"), b -> removeCurrentImage());

            addScrollableWidget(prevImageButton);
            addScrollableWidget(nextImageButton);
            addScrollableWidget(deleteImageButton);
            updateNavButtons();
        }

        this.setInitialFocus(this.nameField);
    }

    @Override
    protected void initContent() {
        this.customFieldWidgets.clear();
        int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();
        int panelSpacing = BuildScreenLayouts.PANEL_SPACING;

        // --- Name Widget ---
        this.nameField = new MultiLineTextFieldWidget(
                this.font, contentX, yPos + 5, contentWidth, BuildScreenLayouts.NAME_FIELD_HEIGHT, build.getName(),
                Component.translatable("gui.buildnotes.placeholder.build_name").getString(),
                1, false
        );

        addScrollableWidget(this.nameField);
        yPos += BuildScreenLayouts.NAME_FIELD_HEIGHT + panelSpacing;

        // --- Coords & Dimension Widgets ---
        int fieldWidth = (contentWidth - panelSpacing) / 2;
        this.coordsField = new MultiLineTextFieldWidget(
                this.font, contentX + 50, yPos, fieldWidth - 50, BuildScreenLayouts.SMALL_FIELD_HEIGHT, build.getCoordinates(),
                Component.translatable("gui.buildnotes.placeholder.coords").getString(), 1, false
        );
        addScrollableWidget(this.coordsField);
        int dimensionX = contentX + fieldWidth + panelSpacing;
        this.dimensionField = new MultiLineTextFieldWidget(
                this.font, dimensionX + 65, yPos, fieldWidth - 65, BuildScreenLayouts.SMALL_FIELD_HEIGHT, build.getDimension(),
                Component.translatable("gui.buildnotes.placeholder.dimension").getString(), 1, false
        );
        addScrollableWidget(this.dimensionField);
        yPos += BuildScreenLayouts.SMALL_FIELD_HEIGHT + panelSpacing;

        // --- Image Gallery Placeholder ---
        if (!build.getImageFileNames().isEmpty()) {
            int galleryHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
            yPos += galleryHeight + panelSpacing;
        }

        // --- Description, Credits, and Custom Fields ---
        yPos += BuildScreenLayouts.LABEL_HEIGHT;
        this.descriptionField = new MultiLineTextFieldWidget(
                this.font, contentX, yPos, contentWidth, BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT, build.getDescription(),
                Component.translatable("gui.buildnotes.placeholder.description").getString(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(this.descriptionField);
        yPos += BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT + panelSpacing;

        yPos += BuildScreenLayouts.LABEL_HEIGHT;
        this.designerField = new MultiLineTextFieldWidget(
                this.font, contentX, yPos, contentWidth, BuildScreenLayouts.CREDITS_FIELD_HEIGHT, build.getCredits(),
                Component.translatable("gui.buildnotes.placeholder.credits").getString(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(this.designerField);
        yPos += BuildScreenLayouts.CREDITS_FIELD_HEIGHT + panelSpacing;

        for (CustomField field : this.build.getCustomFields()) {
            yPos += BuildScreenLayouts.LABEL_HEIGHT;
            int fieldWidgetWidth = contentWidth - BuildScreenLayouts.CUSTOM_FIELD_REMOVE_BTN_WIDTH - panelSpacing;

            MultiLineTextFieldWidget fieldArea = new MultiLineTextFieldWidget(this.font, contentX, yPos, fieldWidgetWidth, BuildScreenLayouts.CUSTOM_FIELD_HEIGHT,
                    field.getContent(), "", Integer.MAX_VALUE, true);

            addScrollableWidget(fieldArea);

            this.customFieldWidgets.put(field, fieldArea);
            DarkButtonWidget removeButton = new DarkButtonWidget(contentX + fieldWidgetWidth + panelSpacing, yPos, BuildScreenLayouts.CUSTOM_FIELD_REMOVE_BTN_WIDTH, 20,
                    Component.translatable("gui.buildnotes.edit.remove_field"),  button -> removeCustomField(field));

            addScrollableWidget(removeButton);
            yPos += BuildScreenLayouts.CUSTOM_FIELD_HEIGHT + panelSpacing;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();
        int panelSpacing = BuildScreenLayouts.PANEL_SPACING;

        UIHelper.drawPanel(context, contentX, yPos, contentWidth, BuildScreenLayouts.NAME_FIELD_HEIGHT);
        yPos += BuildScreenLayouts.NAME_FIELD_HEIGHT + panelSpacing;


        int fieldWidth = (contentWidth - panelSpacing) / 2;
        UIHelper.drawPanel(context, contentX, yPos, fieldWidth, BuildScreenLayouts.SMALL_FIELD_HEIGHT);
        context.text(this.font, Component.translatable("gui.buildnotes.label.coords").withStyle(ChatFormatting.GRAY), contentX + 4, (int)(yPos + (BuildScreenLayouts.SMALL_FIELD_HEIGHT - 8) / 2f + 1), Colors.TEXT_MUTED, false);
        int dimensionX = contentX + fieldWidth + panelSpacing;
        UIHelper.drawPanel(context, dimensionX, yPos, fieldWidth, BuildScreenLayouts.SMALL_FIELD_HEIGHT);
        context.text(this.font, Component.translatable("gui.buildnotes.label.dimension").withStyle(ChatFormatting.GRAY), dimensionX + 4, (int)(yPos + (BuildScreenLayouts.SMALL_FIELD_HEIGHT - 8) / 2f + 1), Colors.TEXT_MUTED, false);
        yPos += BuildScreenLayouts.SMALL_FIELD_HEIGHT + panelSpacing;

        if (!build.getImageFileNames().isEmpty()) {
            int galleryBoxHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
            UIHelper.drawPanel(context, contentX, yPos, contentWidth, galleryBoxHeight);
            String currentImageName = build.getImageFileNames().get(currentImageIndex);
            if (downloadingImages.contains(currentImageName)) {
                context.centeredText(font, Component.translatable("gui.buildnotes.gallery.loading").withStyle(ChatFormatting.YELLOW), this.width / 2, yPos + galleryBoxHeight / 2 - 4, Colors.TEXT_PRIMARY);
            } else {
                ImageData data = getImageDataForCurrentImage();
                if (data != null && data.textureId != null) {
                    int boxWidth = contentWidth - 4;
                    int boxHeight = galleryBoxHeight - 4;
                    float imageAspect = (float) data.width / (float) data.height;
                    float boxAspect = (float) boxWidth / (float) boxHeight;
                    int renderWidth = boxWidth;
                    int renderHeight = boxHeight;
                    if (imageAspect > boxAspect) {
                        renderHeight = (int) (boxWidth / imageAspect);
                    } else {
                        renderWidth = (int) (boxHeight * imageAspect);
                    }
                    int renderX = contentX + 2 + (boxWidth - renderWidth) / 2;
                    int renderY = yPos + 2 + (boxHeight - renderHeight) / 2;
                    context.blit(RenderPipelines.GUI_TEXTURED, data.textureId, renderX, renderY, 0, 0, renderWidth, renderHeight, renderWidth, renderHeight);
                } else {
                    context.centeredText(font, Component.translatable("gui.buildnotes.gallery.error").withStyle(ChatFormatting.RED), this.width / 2, yPos + galleryBoxHeight / 2 - 4, Colors.TEXT_PRIMARY);
                }
            }
            String counter = (currentImageIndex + 1) + " / " + build.getImageFileNames().size();
            int counterWidth = font.width(counter);
            context.text(this.font, counter, contentX + contentWidth - counterWidth - 5, yPos + galleryBoxHeight - 12, Colors.TEXT_PRIMARY, true);
            yPos += galleryBoxHeight + panelSpacing;
        }

        context.text(this.font, Component.translatable("gui.buildnotes.label.description").withStyle(ChatFormatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
        yPos += BuildScreenLayouts.LABEL_HEIGHT;
        UIHelper.drawPanel(context, contentX, yPos, contentWidth, BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT);
        yPos += BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT + panelSpacing;
        context.text(this.font, Component.translatable("gui.buildnotes.label.credits").withStyle(ChatFormatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
        yPos += BuildScreenLayouts.LABEL_HEIGHT;
        UIHelper.drawPanel(context, contentX, yPos, contentWidth, BuildScreenLayouts.CREDITS_FIELD_HEIGHT);
        yPos += BuildScreenLayouts.CREDITS_FIELD_HEIGHT + panelSpacing;
        for (CustomField field : this.build.getCustomFields()) {
            context.text(this.font, Component.translatable(field.getTitle() + ":").withStyle(ChatFormatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
            yPos += BuildScreenLayouts.LABEL_HEIGHT;
            UIHelper.drawPanel(context, contentX, yPos, contentWidth, BuildScreenLayouts.CUSTOM_FIELD_HEIGHT);
            yPos += BuildScreenLayouts.CUSTOM_FIELD_HEIGHT + panelSpacing;
        }
        context.centeredText(this.font, this.title, this.width / 2, 8, Colors.TEXT_PRIMARY);
    }

    // --- Image Management Logic ---
    private void switchImage(int direction) {
        int newIndex = this.currentImageIndex + direction;
        if (newIndex >= 0 && newIndex < build.getImageFileNames().size()) {
            this.currentImageIndex = newIndex;
            updateNavButtons();
        }
    }

    private void removeCurrentImage() {
        if (build.getImageFileNames().isEmpty()) return;
        String fileNameToRemove = build.getImageFileNames().get(currentImageIndex);
        build.getImageFileNames().remove(fileNameToRemove);
        if (currentImageIndex >= build.getImageFileNames().size()) {
            currentImageIndex = Math.max(0, build.getImageFileNames().size() - 1);
        }
        rebuild();
    }

    private void updateNavButtons() {
        if (prevImageButton != null) prevImageButton.active = currentImageIndex > 0;
        if (nextImageButton != null) nextImageButton.active = currentImageIndex < build.getImageFileNames().size() - 1;
    }

    private ImageData getImageDataForCurrentImage() {
        if (build.getImageFileNames().isEmpty()) return null;
        String fileName = build.getImageFileNames().get(currentImageIndex);
        if (textureCache.containsKey(fileName)) {
            return textureCache.get(fileName);
        }
        try {
            Path imagePath = FabricLoader.getInstance().getConfigDir()
                    .resolve("buildnotes").resolve("images")
                    .resolve(build.getId().toString()).resolve(fileName);

            if (Files.exists(imagePath)) {
                try (InputStream stream = Files.newInputStream(imagePath)) {
                    NativeImage image = NativeImage.read(stream);

                    Identifier textureId = Identifier.fromNamespaceAndPath(
                            Buildnotes.MOD_ID, "buildnotes_image_" + build.getId() + "_" + fileName.hashCode());
                    DynamicTexture texture = new DynamicTexture (textureId::toString, image);

                    this.minecraft.getTextureManager().register(textureId, texture);

                    ImageData data = new ImageData(textureId, image.getWidth(), image.getHeight());
                    textureCache.put(fileName, data);
                    return data;
                }
            }
            else {
                // --- Only request images for SERVER-scoped builds when on a dedicated server ---
                boolean isDedicatedServer = !this.minecraft.isSingleplayer();
                if (build.getScope() == Scope.SERVER && isDedicatedServer) {
                    // Image does NOT exist, request it from the server
                    if (!downloadingImages.contains(fileName)) {
                        downloadingImages.add(fileName);
                        ClientImageTransferManager.requestImage(build.getId(), fileName, () -> {
                            // This is the CALLBACK! It runs when the download is finished (success or fail).
                            this.minecraft.execute(() -> downloadingImages.remove(fileName));
                        });
                    }
                }
                // Return null for now, the render method will see this and show "Loading..."
                return null;
            }
        } catch (Exception e) {
            textureCache.put(fileName, null);
        }
        return null;
    }

    // --- Overridden & Helper Methods ---
    @Override
    public void onClose() {
        // Clean up textures to prevent memory leaks
        textureCache.values().forEach(data -> {
            if (data != null && data.textureId != null) {
                minecraft.getTextureManager().release(data.textureId);
            }
        });
        saveBuild();
        super.onClose();
    }

    private void rebuild() {
        saveBuild();
        this.open(new EditBuildScreen(this.parent, this.build));
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


    private void insertTextAtLastFocus(String Component) {
        if (this.lastFocusedTextField != null) {
            this.lastFocusedTextField.insertText(Component);
            this.setFocused(this.lastFocusedTextField);
        } else if (this.descriptionField != null) { // Fallback to a default field
            this.descriptionField.insertText(Component);
            this.setFocused(this.descriptionField);
        }
    }

    private void insertCoords() {
        if (this.minecraft.player == null) return;
        String coords = String.format("%.0f, %.0f, %.0f", this.minecraft.player.getX(), this.minecraft.player.getY(), this.minecraft.player.getZ());
        insertTextAtLastFocus(coords);
    }

    private void insertDimension() {
        if (this.minecraft.player == null) return;
        String dim = this.minecraft.player.level().dimension().identifier().toString();
        insertTextAtLastFocus(dim);
    }

    private void insertBiome() {
        if (this.minecraft.player == null || this.minecraft.level == null) return;

        BlockPos playerPos = this.minecraft.player.blockPosition();
        Holder<Biome> biomeEntry = this.minecraft.level.getBiome(playerPos);

        String biomeId = biomeEntry.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Identifier::toString)
                .orElse("minecraft:unknown");
        insertTextAtLastFocus(biomeId);
    }

    private void openImageSelectionDialog() {
        new Thread(() -> {
            String selectedFiles = TinyFileDialogs.tinyfd_openFileDialog("Select Image(s)", null, null, "Image Files (*.png, *.jpg, *.jpeg)", true);
            minecraft.execute(() -> {
                if (selectedFiles != null) {
                    processSelectedFiles(selectedFiles.split("\\|"));
                }
                long handle = minecraft.getWindow().handle();
                GLFW.glfwRestoreWindow(handle);
                GLFW.glfwFocusWindow(handle);
            });
        }).start();
    }

    private void processSelectedFiles(String[] paths) {
        new Thread(() -> {
            for (String pathStr : paths) {
                try {
                    Path sourcePath = Path.of(pathStr);
                    String fileName = sourcePath.getFileName().toString().toLowerCase();
                    if (!(fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))) continue;
                    Path destDir = FabricLoader.getInstance().getConfigDir().resolve("buildnotes").resolve("images").resolve(build.getId().toString());
                    Files.createDirectories(destDir);
                    Path destPath = destDir.resolve(fileName);
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    if (!build.getImageFileNames().contains(fileName)) {
                        build.getImageFileNames().add(fileName);
                    }
                } catch (Exception e) {
                    // Log error
                }
            }
            minecraft.execute(this::rebuild);
        }).start();
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

    private void cycleScope() {
        Scope currentScope = build.getScope();
        if (ClientSession.isOnServer() && ClientSession.hasEditPermission()) {
            if (currentScope == Scope.WORLD) build.setScope(Scope.GLOBAL);
            else if (currentScope == Scope.GLOBAL) build.setScope(Scope.SERVER);
            else build.setScope(Scope.WORLD);
        } else {
            build.setScope(currentScope == Scope.WORLD ? Scope.GLOBAL : Scope.WORLD);
        }
    }

    private Component getScopeButtonText() {
        Component scopeName;
        Scope currentScope = build.getScope();
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

    @Override
    public void setFocused(GuiEventListener focused) {
        super.setFocused(focused);
        if (focused instanceof MultiLineTextFieldWidget widget) {
            this.lastFocusedTextField = widget;
        }
    }

    // --- Inner Class for RequestFieldTitleScreen (Unchanged) ---
    private static class RequestFieldTitleScreen extends BaseScreen {
        private final Consumer<String> onConfirm;
        private EditBox titleField;

        protected RequestFieldTitleScreen(Screen parent, Consumer<String> onConfirm) {
            super(Component.translatable("gui.buildnotes.prompt.field_title"), parent);
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            int panelH = 100;
            int panelW = 200;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            this.titleField = new EditBox(this.font, panelX + 10, panelY + 20, panelW - 20, 20, Component.literal(""));
            this.addWidget(this.titleField);

            List<Component> buttonTexts = List.of(
                    Component.translatable("gui.buildnotes.confirm_button"),
                    Component.translatable("gui.buildnotes.cancel_button")
            );

            int buttonsY = panelY + panelH - UIHelper.BUTTON_HEIGHT - UIHelper.OUTER_PADDING;
            UIHelper.createButtonRow(this, buttonsY, buttonTexts, (index, x, width) -> {
                if (index == 0) {
                    this.addRenderableWidget(new DarkButtonWidget(x, buttonsY, width, 20, buttonTexts.get(0), button -> {
                        this.onConfirm.accept(this.titleField.getValue());
                        this.open(this.parent);
                    }));
                } else {
                    this.addRenderableWidget(new DarkButtonWidget(x, buttonsY, width, 20, buttonTexts.get(1), button -> this.open(parent)));
                }
            });



            this.setInitialFocus(this.titleField);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            //parent.render(context, -1, -1, delta);
            int panelW = 200;
            int panelH = 100;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;

            panelH = panelH - (UIHelper.BUTTON_HEIGHT + (UIHelper.OUTER_PADDING * 2));
            UIHelper.drawPanel(context, panelX, panelY, panelW, panelH);
            super.extractRenderState(context, mouseX, mouseY, delta);

            context.centeredText(this.font, this.title, this.width / 2, panelY + 8, Colors.TEXT_PRIMARY);

            this.titleField.extractRenderState(context, mouseX, mouseY, delta);
        }
    }
}