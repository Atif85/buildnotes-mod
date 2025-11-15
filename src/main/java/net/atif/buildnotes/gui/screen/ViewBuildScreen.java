package net.atif.buildnotes.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.atif.buildnotes.Buildnotes;
import net.atif.buildnotes.client.ClientImageTransferManager;
import net.atif.buildnotes.data.Build;
import net.atif.buildnotes.data.CustomField;
import net.atif.buildnotes.data.DataManager;
import net.atif.buildnotes.data.*;
import net.atif.buildnotes.gui.helper.UIHelper;
import net.atif.buildnotes.gui.widget.DarkButtonWidget;
import net.atif.buildnotes.gui.widget.ReadOnlyMultiLineTextFieldWidget;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ViewBuildScreen extends ScrollableScreen {

    private final Build build;

    private record ImageData(Identifier textureId, int width, int height) {}
    private int currentImageIndex = 0;
    private final Map<String, ImageData> textureCache = new HashMap<>();
    private final Set<String> downloadingImages = new HashSet<>();

    private DarkButtonWidget prevImageButton;
    private DarkButtonWidget nextImageButton;

    public ViewBuildScreen(Screen parent, Build build) {
        super(Text.literal(build.getName()), parent);
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
        addScrollableWidget(coordsArea);

        // Dimension Widget (positioned after the label)
        int dimensionX = contentX + fieldWidth + panelSpacing;
        int dimensionTextX = dimensionX + 65;
        ReadOnlyMultiLineTextFieldWidget dimensionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, dimensionTextX, yPos, fieldWidth - 65, smallFieldHeight,
                build.getDimension(), 1, false
        );
        addScrollableWidget(dimensionArea);
        yPos += smallFieldHeight + panelSpacing;

        if (!build.getImageFileNames().isEmpty()) {
            int galleryHeight = (int) (contentWidth * (9.0 / 16.0)); // 16:9 aspect ratio
            yPos += galleryHeight + panelSpacing;
        }

        // --- DESCRIPTION WIDGET ---
        int descriptionHeight = 80;
        ReadOnlyMultiLineTextFieldWidget descriptionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + labelHeight, contentWidth, descriptionHeight,
                build.getDescription(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(descriptionArea);
        yPos += descriptionHeight + labelHeight + panelSpacing;

        // --- CREDITS WIDGET ---
        int creditsHeight = 40;
        ReadOnlyMultiLineTextFieldWidget creditsArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + labelHeight, contentWidth, creditsHeight,
                build.getCredits(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(creditsArea);
        yPos += creditsHeight + labelHeight + panelSpacing;

        // --- CUSTOM FIELD WIDGETS ---
        for (CustomField field : build.getCustomFields()) {
            int fieldHeight = 40;
            ReadOnlyMultiLineTextFieldWidget fieldArea = new ReadOnlyMultiLineTextFieldWidget(
                    this.textRenderer, contentX, yPos + labelHeight, contentWidth, fieldHeight,
                    field.getContent(), Integer.MAX_VALUE, true
            );
            addScrollableWidget(fieldArea);
            yPos += fieldHeight + labelHeight + panelSpacing;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    protected void init() {
        super.init();

        // Use UIHelper to create the bottom 3 action buttons
        int buttonsY = this.height - UIHelper.BUTTON_HEIGHT - UIHelper.BOTTOM_PADDING;
        UIHelper.createBottomButtonRow(this, buttonsY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.delete_button"), button -> confirmDelete()));
                case 1 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.edit_button"), button -> open(new EditBuildScreen(this.parent, this.build))));
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.close_button"), button -> this.open(this.parent)));
            }
        });

        if (!build.getImageFileNames().isEmpty()) {
            int contentWidth = (int) (this.width * 0.6);
            int contentX = (this.width - contentWidth) / 2;
            int galleryHeight = (int) (contentWidth * (9.0 / 16.0));
            int galleryY = getTopMargin() + 25 + 5 + 20 + 5; // Y pos after title and coords
            int navButtonY = galleryY + (galleryHeight - 20) / 2;

            prevImageButton = new DarkButtonWidget(contentX - 25, navButtonY, 20, 20, Text.literal("<"), b -> switchImage(-1));
            nextImageButton = new DarkButtonWidget(contentX + contentWidth + 5, navButtonY, 20, 20, Text.literal(">"), b -> switchImage(1));
            addScrollableWidget(prevImageButton);
            addScrollableWidget(nextImageButton);
            updateNavButtons();
        }
    }

        @Override
        protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
            int contentWidth = (int) (this.width * 0.6);
            int contentX = (this.width - contentWidth) / 2;
            int yPos = getTopMargin();

            final int panelSpacing = 5;
            final int labelHeight = 12;

            // --- TITLE ---
            int titlePanelHeight = 25;
            context.fill(contentX, yPos, contentX + contentWidth, yPos + titlePanelHeight, 0x77000000);
            yPos += titlePanelHeight + panelSpacing;

            // --- COORDS & DIMENSION ---
            int smallFieldHeight = 20;
            int fieldWidth = (contentWidth - panelSpacing) / 2;

            // Backgrounds and Labels only
            UIHelper.drawPanel(context, contentX, yPos, fieldWidth, smallFieldHeight);
            context.drawText(this.textRenderer ,Text.literal("Coords: ").formatted(Formatting.GRAY), contentX + 4, (int)(yPos + (smallFieldHeight - 8) / 2f + 1), 0xCCCCCC, false);

            int dimensionX = contentX + fieldWidth + panelSpacing;
            UIHelper.drawPanel(context, dimensionX, yPos, fieldWidth, smallFieldHeight);
            context.drawText(this.textRenderer, Text.literal("Dimension: ").formatted(Formatting.GRAY), dimensionX + 4, (int)(yPos + (smallFieldHeight - 8) / 2f + 1), 0xCCCCCC, false);
            yPos += smallFieldHeight + panelSpacing;

            if (!build.getImageFileNames().isEmpty()) {
                int galleryBoxHeight = (int) (contentWidth * (9.0 / 16.0));
                context.fill(contentX, yPos, contentX + contentWidth, yPos + galleryBoxHeight, 0x77000000);

                String currentImageName = build.getImageFileNames().get(currentImageIndex);
                if (downloadingImages.contains(currentImageName)) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal("Loading image...").formatted(Formatting.YELLOW), this.width / 2, yPos + galleryBoxHeight / 2 - 4, 0xFFFFFF);
                } else {
                    ImageData data = getImageDataForCurrentImage();
                    if (data != null && data.textureId != null) {
                        RenderSystem.setShaderTexture(0, data.textureId);
                        RenderSystem.enableBlend();

                        // --- ASPECT RATIO LOGIC ---
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

                        context.drawTexture(RenderLayer::getGuiTextured, data.textureId, renderX, renderY, 0, 0, renderWidth, renderHeight, renderWidth, renderHeight);
                        RenderSystem.disableBlend();
                    } else {
                        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Error or missing image").formatted(Formatting.RED), this.width / 2, yPos + galleryBoxHeight / 2 - 4, 0xFFFFFF);
                    }
                }
                String counter = (currentImageIndex + 1) + " / " + build.getImageFileNames().size();
                int counterWidth = textRenderer.getWidth(counter);
                context.drawText(this.textRenderer, counter, contentX + contentWidth - counterWidth - 5, yPos + galleryBoxHeight - 12, 0xFFFFFF, false);

                yPos += galleryBoxHeight + panelSpacing;
            }

            // --- DYNAMIC CONTENT ---
            int descriptionHeight = 80;
            context.drawText(this.textRenderer, Text.literal("Description:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF, false);
            context.fill(contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + descriptionHeight, 0x77000000);
            yPos += descriptionHeight + labelHeight + panelSpacing;

            int creditsHeight = 40;
            context.drawText(this.textRenderer, Text.literal("Credits:").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF, false);
            context.fill(contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + creditsHeight, 0x77000000);
            yPos += creditsHeight + labelHeight + panelSpacing;

            for (CustomField field : build.getCustomFields()) {
                int fieldHeight = 40;
                context.drawText(this.textRenderer, Text.literal(field.getTitle() + ":").formatted(Formatting.GRAY), contentX, yPos, 0xFFFFFF, false);
                context.fill(contentX, yPos + labelHeight, contentX + contentWidth, yPos + labelHeight + fieldHeight, 0x77000000);
                yPos += fieldHeight + labelHeight + panelSpacing;
            }
        }


    private void switchImage(int direction) {
        int newIndex = this.currentImageIndex + direction;
        if (newIndex >= 0 && newIndex < build.getImageFileNames().size()) {
            this.currentImageIndex = newIndex;
            updateNavButtons();
        }
    }

    private void updateNavButtons() {
        if (prevImageButton != null) {
            prevImageButton.active = currentImageIndex > 0;
        }
        if (nextImageButton != null) {
            nextImageButton.active = currentImageIndex < build.getImageFileNames().size() - 1;
        }
    }

    private ImageData getImageDataForCurrentImage() {
        if (build.getImageFileNames().isEmpty()) return null;

        String fileName = build.getImageFileNames().get(currentImageIndex);
        if (textureCache.containsKey(fileName)) {
            return textureCache.get(fileName);
        }

        try {
            Path imagePath = FabricLoader.getInstance().getConfigDir()
                    .resolve("buildnotes")
                    .resolve("images")
                    .resolve(build.getId().toString())
                    .resolve(fileName);

            if (Files.exists(imagePath)) {
                try (InputStream stream = Files.newInputStream(imagePath)) {
                    NativeImage image = NativeImage.read(stream);
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

                    Identifier textureId = Identifier.of(Buildnotes.MOD_ID, "buildnotes_image_" + build.getId() + "_" + fileName.hashCode());
                    this.client.getTextureManager().registerTexture(textureId, texture);

                    ImageData data = new ImageData(textureId, image.getWidth(), image.getHeight());
                    textureCache.put(fileName, data);
                    return data;
                }
            } else {
                // --- Only request images for SERVER-scoped builds when on a dedicated server ---
                boolean isDedicatedServer = this.client != null && !this.client.isIntegratedServerRunning();
                if (build.getScope() == Scope.SERVER && isDedicatedServer) {
                    // Image does NOT exist, request it from the server
                    if (!downloadingImages.contains(fileName)) {
                        downloadingImages.add(fileName);
                        ClientImageTransferManager.requestImage(build.getId(), fileName, () -> {
                            // This is the CALLBACK! It runs when the download is finished (success or fail).
                            this.client.execute(() -> downloadingImages.remove(fileName));
                        });
                    }
                }
                return null; // Return null to signal that it's loading
            }
        } catch (Exception e) {
            textureCache.put(fileName, null); // Cache failure
        }
        return null;
    }

    private void confirmDelete() {
        Runnable onConfirm = () -> {
            DataManager.getInstance().deleteBuild(this.build);
            this.close();
        };
        this.showConfirm(Text.literal("Delete build \"" + build.getName() + "\"?"), onConfirm);
    }

    @Override
    public void close() {
        textureCache.values().forEach(data -> {
            if (data != null && data.textureId != null) {
                client.getTextureManager().destroyTexture(data.textureId);
            }
        });
        super.close();
    }

}