package net.atif.buildnotes.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.client.ClientImageTransferManager;
import net.atif.buildnotes.client.ClientSession;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.Scope;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.MultiLineTextFieldWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
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
        super(Text.translatable("gui.buildnotes.edit_build_title"), parent);
        this.build = build;
    }

    @Override
    protected void init() {
        super.init(); // calls initContent()

        // --- BOTTOM BUTTON ROW ---
        int buttonsY = this.height - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;
        List<Text> bottomTexts = List.of(
                Text.translatable("gui.buildnotes.save_button"),
                Text.translatable("gui.buildnotes.close_button")
        );
        UIHelper.createBottomButtonRow(this, buttonsY, bottomTexts, (index, x, width) -> {
            if (index == 0) {
                this.addDrawableChild(new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT, bottomTexts.get(0), button -> saveBuild()));
            } else {
                this.addDrawableChild(new DarkButtonWidget(x, buttonsY, width, UIHelper.BUTTON_HEIGHT, bottomTexts.get(1), button -> this.close()));
            }
        });

        // --- TOP BUTTON ROW ---
        int topRowY = buttonsY - UIHelper.BUTTON_HEIGHT - 5;
        List<Text> topTexts = List.of(
                Text.translatable("gui.buildnotes.edit.coords"),
                Text.translatable("gui.buildnotes.edit.dimension"),
                Text.translatable("gui.buildnotes.edit.biome"),
                Text.translatable("gui.buildnotes.edit.add_images"),
                Text.translatable("gui.buildnotes.edit.add_field"),
                getScopeButtonText()
        );
        UIHelper.createBottomButtonRow(this, topRowY, topTexts, (index, x, width) -> {
            switch (index) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(0), b -> insertCoords()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(1), b -> insertDimension()));
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(2), b -> insertBiome()));
                case 3 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(3), b -> openImageSelectionDialog()));
                case 4 -> this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(4), b -> {
                    saveBuild();
                    this.open(new RequestFieldTitleScreen(this, this::addCustomField));
                }));
                case 5 ->
                        this.addDrawableChild(new DarkButtonWidget(x, topRowY, width, UIHelper.BUTTON_HEIGHT, topTexts.get(5), button -> {
                            saveBuild();
                            cycleScope();
                            this.init(this.client, this.width, this.height);
                        }));
            }
        });

        // --- IMAGE GALLERY WIDGETS ---
        if (!build.getImageFileNames().isEmpty()) {
            int contentWidth = (int) (this.width * 0.6);
            int contentX = (this.width - contentWidth) / 2;
            int galleryHeight = (int) (contentWidth * (9.0 / 16.0));
            int galleryY = getTopMargin() + 25 + 5 + 20 + 5;
            int navButtonY = galleryY + (galleryHeight - 20) / 2;

            prevImageButton = new DarkButtonWidget(contentX - 25, navButtonY, 20, 20, Text.translatable("gui.buildnotes.gallery.previous"), b -> switchImage(-1));
            nextImageButton = new DarkButtonWidget(contentX + contentWidth + 5, navButtonY, 20, 20, Text.translatable("gui.buildnotes.gallery.next"), b -> switchImage(1));
            DarkButtonWidget deleteImageButton = new DarkButtonWidget(contentX + contentWidth - 22, galleryY + 2, 20, 20, Text.translatable("gui.buildnotes.gallery.delete"), b -> removeCurrentImage());

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
        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();
        final int panelSpacing = 5;
        final int labelHeight = 12;

        // --- Name Widget ---
        this.nameField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + 5, contentWidth, 25, build.getName(),
                Text.translatable("gui.buildnotes.placeholder.build_name").getString(),
                1, false
        );

        this.nameField.setInternalScissoring(false);
        addScrollableWidget(this.nameField);
        yPos += 25 + panelSpacing;

        // --- Coords & Dimension Widgets ---
        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;
        this.coordsField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX + 50, yPos, fieldWidth - 50, smallFieldHeight, build.getCoordinates(),
                Text.translatable("gui.buildnotes.placeholder.coords").getString(), 1, false
        );
        this.coordsField.setInternalScissoring(false);
        addScrollableWidget(this.coordsField);
        int dimensionX = contentX + fieldWidth + panelSpacing;
        this.dimensionField = new MultiLineTextFieldWidget(
                this.textRenderer, dimensionX + 65, yPos, fieldWidth - 65, smallFieldHeight, build.getDimension(),
                Text.translatable("gui.buildnotes.placeholder.dimension").getString(), 1, false
        );
        this.dimensionField.setInternalScissoring(false);
        addScrollableWidget(this.dimensionField);
        yPos += smallFieldHeight + panelSpacing;

        // --- Image Gallery Placeholder ---
        if (!build.getImageFileNames().isEmpty()) {
            int galleryHeight = (int) (contentWidth * (9.0 / 16.0)); // 16:9 aspect ratio
            yPos += galleryHeight + panelSpacing;
        }

        // --- Description, Credits, and Custom Fields ---
        yPos += labelHeight;
        this.descriptionField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos, contentWidth, 80, build.getDescription(),
                Text.translatable("gui.buildnotes.placeholder.description").getString(), Integer.MAX_VALUE, true
        );
        this.descriptionField.setInternalScissoring(false);
        addScrollableWidget(this.descriptionField);
        yPos += 80 + panelSpacing;

        yPos += labelHeight;
        this.designerField = new MultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos, contentWidth, 40, build.getCredits(),
                Text.translatable("gui.buildnotes.placeholder.credits").getString(), Integer.MAX_VALUE, true
        );
        this.designerField.setInternalScissoring(false);
        addScrollableWidget(this.designerField);
        yPos += 40 + panelSpacing;

        for (CustomField field : this.build.getCustomFields()) {
            yPos += labelHeight;
            int customRemoveBtnWidth = 20;
            int fieldWidgetWidth = contentWidth - customRemoveBtnWidth - panelSpacing;

            MultiLineTextFieldWidget fieldArea = new MultiLineTextFieldWidget(this.textRenderer, contentX, yPos, fieldWidgetWidth, 40,
                    field.getContent(), "", Integer.MAX_VALUE, true);

            fieldArea.setInternalScissoring(false);
            addScrollableWidget(fieldArea);

            this.customFieldWidgets.put(field, fieldArea);
            DarkButtonWidget removeButton = new DarkButtonWidget(contentX + fieldWidgetWidth + panelSpacing, yPos, customRemoveBtnWidth, 20,
                    Text.translatable("gui.buildnotes.edit.remove_field"),  button -> removeCustomField(field));

            addScrollableWidget(removeButton);
            yPos += 40 + panelSpacing;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    protected void renderContent(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int contentWidth = (int) (this.width * 0.6);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();
        final int panelSpacing = 5;
        final int labelHeight = 12;

        UIHelper.drawPanel(matrices, contentX, yPos, contentWidth, 25);
        yPos += 25 + panelSpacing;
        int smallFieldHeight = 20;
        int fieldWidth = (contentWidth - panelSpacing) / 2;
        UIHelper.drawPanel(matrices, contentX, yPos, fieldWidth, smallFieldHeight);
        this.textRenderer.draw(matrices, Text.translatable("gui.buildnotes.label.coords").formatted(Formatting.GRAY), contentX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);
        int dimensionX = contentX + fieldWidth + panelSpacing;
        UIHelper.drawPanel(matrices, dimensionX, yPos, fieldWidth, smallFieldHeight);
        this.textRenderer.draw(matrices, Text.translatable("gui.buildnotes.label.dimension").formatted(Formatting.GRAY), dimensionX + 4, yPos + (smallFieldHeight - 8) / 2f + 1, 0xCCCCCC);
        yPos += smallFieldHeight + panelSpacing;

        if (!build.getImageFileNames().isEmpty()) {
            int galleryBoxHeight = (int) (contentWidth * (9.0 / 16.0));
            fill(matrices, contentX, yPos, contentX + contentWidth, yPos + galleryBoxHeight, 0x77000000);
            String currentImageName = build.getImageFileNames().get(currentImageIndex);
            if (downloadingImages.contains(currentImageName)) {
                drawCenteredText(matrices, textRenderer, Text.translatable("gui.buildnotes.gallery.loading").formatted(Formatting.YELLOW), this.width / 2, yPos + galleryBoxHeight / 2 - 4, 0xFFFFFF);
            } else {
                ImageData data = getImageDataForCurrentImage();
                if (data != null && data.textureId != null) {
                    RenderSystem.setShaderTexture(0, data.textureId);
                    RenderSystem.enableBlend();
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
                    DrawableHelper.drawTexture(matrices, renderX, renderY, 0, 0, renderWidth, renderHeight, renderWidth, renderHeight);
                    RenderSystem.disableBlend();
                } else {
                    drawCenteredText(matrices, textRenderer, Text.translatable("gui.buildnotes.gallery.error").formatted(Formatting.RED), this.width / 2, yPos + galleryBoxHeight / 2 - 4, 0xFFFFFF);
                }
            }
            String counter = (currentImageIndex + 1) + " / " + build.getImageFileNames().size();
            int counterWidth = textRenderer.getWidth(counter);
            textRenderer.draw(matrices, counter, contentX + contentWidth - counterWidth - 5, yPos + galleryBoxHeight - 12, 0xFFFFFF);
            yPos += galleryBoxHeight + panelSpacing;
        }

        this.textRenderer.draw(matrices, Text.translatable("gui.buildnotes.label.description").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        yPos += labelHeight;
        UIHelper.drawPanel(matrices, contentX, yPos, contentWidth, 80);
        yPos += 80 + panelSpacing;
        this.textRenderer.draw(matrices, Text.translatable("gui.buildnotes.label.credits").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
        yPos += labelHeight;
        UIHelper.drawPanel(matrices, contentX, yPos, contentWidth, 40);
        yPos += 40 + panelSpacing;
        for (CustomField field : this.build.getCustomFields()) {
            this.textRenderer.draw(matrices, Text.translatable(field.getTitle() + ":").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF);
            yPos += labelHeight;
            UIHelper.drawPanel(matrices, contentX, yPos, contentWidth, 40);
            yPos += 40 + panelSpacing;
        }
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
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
            Path imagePath = FabricLoader.getInstance().getConfigDir().resolve("buildnotes").resolve("images").resolve(build.getId().toString()).resolve(fileName);
            if (Files.exists(imagePath)) {
                try (InputStream stream = Files.newInputStream(imagePath)) {
                    NativeImage image = NativeImage.read(stream);
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                    Identifier textureId = this.client.getTextureManager().registerDynamicTexture("buildnotes_image_" + build.getId() + "_" + fileName.hashCode(), texture);
                    ImageData data = new ImageData(textureId, image.getWidth(), image.getHeight());
                    textureCache.put(fileName, data);
                    return data;
                }
            }
            else {
                // --- Only request images for SERVER-scoped builds when on a dedicated server ---
                boolean isDedicatedServer = this.client != null && !this.client.isIntegratedServerRunning();
                if (build.getScope() == Scope.SERVER && isDedicatedServer) {
                    // Image does NOT exist, request it from the server
                    if (!downloadingImages.contains(fileName)) {
                        downloadingImages.add(fileName);
                        ClientImageTransferManager.requestImage(build.getId(), fileName, () -> {
                            // This is the CALLBACK! It runs when the download is finished (success or fail).
                            this.client.execute(() -> {
                                downloadingImages.remove(fileName);
                            });
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
    public void close() {
        // Clean up textures to prevent memory leaks
        textureCache.values().forEach(data -> {
            if (data != null && data.textureId != null) {
                client.getTextureManager().destroyTexture(data.textureId);
            }
        });
        saveBuild();
        super.close();
    }

    private void rebuild() {
        saveBuild();
        this.open(new EditBuildScreen(this.parent, this.build));
    }

    @Override
    protected int getTopMargin() { return 20; }

    @Override
    protected int getBottomMargin() { return 70; }

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


    private void insertTextAtLastFocus(String text) {
        if (this.lastFocusedTextField != null) {
            this.lastFocusedTextField.insertText(text);
            this.setFocused(this.lastFocusedTextField);
        } else if (this.descriptionField != null) { // Fallback to a default field
            this.descriptionField.insertText(text);
            this.setFocused(this.descriptionField);
        }
    }

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
        RegistryEntry<Biome> biomeEntry = this.client.world.getBiome(playerPos);
        String biomeId = biomeEntry.getKey().map(RegistryKey::getValue).map(Identifier::toString).orElse("minecraft:unknown");
        insertTextAtLastFocus(biomeId);
    }

    private void openImageSelectionDialog() {
        new Thread(() -> {
            String selectedFiles = TinyFileDialogs.tinyfd_openFileDialog("Select Image(s)", null, null, "Image Files (*.png, *.jpg, *.jpeg)", true);
            client.execute(() -> {
                if (selectedFiles != null) {
                    processSelectedFiles(selectedFiles.split("\\|"));
                }
                if (client != null) {
                    long handle = client.getWindow().getHandle();
                    GLFW.glfwRestoreWindow(handle);
                    GLFW.glfwFocusWindow(handle);
                }
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
            client.execute(this::rebuild);
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

    private Text getScopeButtonText() {
        Text scopeName;
        Scope currentScope = build.getScope();
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

    @Override
    public void setFocused(Element focused) {
        super.setFocused(focused);
        if (focused instanceof MultiLineTextFieldWidget widget) {
            this.lastFocusedTextField = widget;
        }
    }

    // --- Inner Class for RequestFieldTitleScreen (Unchanged) ---
    private static class RequestFieldTitleScreen extends BaseScreen {
        private final Consumer<String> onConfirm;
        private TextFieldWidget titleField;

        protected RequestFieldTitleScreen(Screen parent, Consumer<String> onConfirm) {
            super(Text.translatable("gui.buildnotes.prompt.field_title"), parent);
            this.onConfirm = onConfirm;
        }

        @Override
        protected void init() {
            super.init();
            int panelW = 200;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - 100) / 2;

            this.titleField = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 20, panelW - 20, 20, Text.of(""));
            this.addSelectableChild(this.titleField);

            int buttonsY = panelY + 60;
            int btnStartX = (this.width - ((85 * 2) + UIHelper.BUTTON_SPACING)) / 2;
            this.addDrawableChild(new DarkButtonWidget(btnStartX, buttonsY, 85, 20, Text.translatable("gui.buildnotes.confirm_button"), button -> {
                this.onConfirm.accept(this.titleField.getText());
                this.open(this.parent);
            }));
            this.addDrawableChild(new DarkButtonWidget(btnStartX + 95, buttonsY, 85, 20, Text.translatable("gui.buildnotes.cancel_button"), button -> this.open(parent)));
            this.setInitialFocus(this.titleField);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            parent.render(matrices, -1, -1, delta);
            int panelW = 200;
            int panelH = 100;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;
            UIHelper.drawPanel(matrices, panelX, panelY, panelW, panelH);

            drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, panelY + 8, 0xFFFFFF);

            this.titleField.render(matrices, mouseX, mouseY, delta);

            super.render(matrices, mouseX, mouseY, delta);
        }
    }
}