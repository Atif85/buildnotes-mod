package net.atif.buildnotes.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
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
    protected int getBottomMargin() { return UIHelper.BUTTON_HEIGHT + UIHelper.OUTER_PADDING * 2; }

    @Override
    protected void initContent() {
        int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
        int contentX = (this.width - contentWidth) / 2;
        int yPos = getTopMargin();

        // --- TITLE WIDGET ---
        ReadOnlyMultiLineTextFieldWidget titleArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + 5, contentWidth, BuildScreenLayouts.NAME_FIELD_HEIGHT,
                this.title.getString(), 1, false
        );
        addScrollableWidget(titleArea);
        yPos += BuildScreenLayouts.NAME_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

        // --- COORDS & DIMENSION WIDGETS ---
        int fieldWidth = (contentWidth - BuildScreenLayouts.PANEL_SPACING) / 2;

        // Coords Widget (positioned after the label)
        int coordsTextX = contentX + 50;
        ReadOnlyMultiLineTextFieldWidget coordsArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, coordsTextX, yPos, fieldWidth - 50, BuildScreenLayouts.SMALL_FIELD_HEIGHT,
                build.getCoordinates(), 1, false
        );
        addScrollableWidget(coordsArea);

        // Dimension Widget (positioned after the label)
        int dimensionX = contentX + fieldWidth + BuildScreenLayouts.PANEL_SPACING;
        int dimensionTextX = dimensionX + 65;
        ReadOnlyMultiLineTextFieldWidget dimensionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, dimensionTextX, yPos, fieldWidth - 65, BuildScreenLayouts.SMALL_FIELD_HEIGHT,
                build.getDimension(), 1, false
        );
        addScrollableWidget(dimensionArea);
        yPos += BuildScreenLayouts.SMALL_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

        if (!build.getImageFileNames().isEmpty()) {
            int galleryHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
            yPos += galleryHeight + BuildScreenLayouts.PANEL_SPACING;
        }

        // --- DESCRIPTION WIDGET ---
        ReadOnlyMultiLineTextFieldWidget descriptionArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT,
                build.getDescription(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(descriptionArea);
        yPos += BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

        // --- CREDITS WIDGET ---
        ReadOnlyMultiLineTextFieldWidget creditsArea = new ReadOnlyMultiLineTextFieldWidget(
                this.textRenderer, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.CREDITS_FIELD_HEIGHT,
                build.getCredits(), Integer.MAX_VALUE, true
        );
        addScrollableWidget(creditsArea);
        yPos += BuildScreenLayouts.CREDITS_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

        // --- CUSTOM FIELD WIDGETS ---
        for (CustomField field : build.getCustomFields()) {
            ReadOnlyMultiLineTextFieldWidget fieldArea = new ReadOnlyMultiLineTextFieldWidget(
                    this.textRenderer, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.CUSTOM_FIELD_HEIGHT,
                    field.getContent(), Integer.MAX_VALUE, true
            );
            addScrollableWidget(fieldArea);
            yPos += BuildScreenLayouts.CUSTOM_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;
        }

        this.totalContentHeight = yPos;
    }

    @Override
    protected void init() {
        super.init();

        boolean canEdit = !(this.build.getScope() == Scope.SERVER && !ClientSession.hasEditPermission());

        // Use UIHelper to create the bottom 3 action buttons
        int buttonsY = UIHelper.getBottomButtonY(this);
        UIHelper.createButtonRow(this, buttonsY, 3, x -> {
            int idx = (x - UIHelper.getCenteredButtonStartX(this.width, 3)) / (UIHelper.BUTTON_WIDTH + UIHelper.BUTTON_SPACING);
            switch (idx) {
                case 0 -> {
                    DarkButtonWidget deleteButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                            Text.translatable("gui.buildnotes.delete_button"), button -> confirmDelete());
                    deleteButton.active = canEdit;
                    this.addDrawableChild(deleteButton);
                }
                case 1 -> {
                    DarkButtonWidget editButton = new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                            Text.translatable("gui.buildnotes.edit_button"), button -> open(new EditBuildScreen(this.parent, this.build)));
                    editButton.active = canEdit;
                    this.addDrawableChild(editButton);
                }
                case 2 -> this.addDrawableChild(new DarkButtonWidget(x, buttonsY, UIHelper.BUTTON_WIDTH, UIHelper.BUTTON_HEIGHT,
                        Text.translatable("gui.buildnotes.close_button"), button -> this.open(this.parent)));
            }
        });

        if (!build.getImageFileNames().isEmpty()) {
            int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
            int contentX = (this.width - contentWidth) / 2;
            int galleryHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
            int galleryY = getTopMargin() + BuildScreenLayouts.NAME_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING + BuildScreenLayouts.SMALL_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING;
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
            int contentWidth = (int) (this.width * BuildScreenLayouts.CONTENT_WIDTH_RATIO);
            int contentX = (this.width - contentWidth) / 2;
            int yPos = getTopMargin();

            // --- TITLE ---
            UIHelper.drawPanel(context, contentX, yPos, contentWidth, BuildScreenLayouts.NAME_FIELD_HEIGHT);
            yPos += BuildScreenLayouts.NAME_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

            // --- COORDS & DIMENSION ---
            int fieldWidth = (contentWidth - BuildScreenLayouts.PANEL_SPACING) / 2;

            // Backgrounds and Labels only
            UIHelper.drawPanel(context, contentX, yPos, fieldWidth, BuildScreenLayouts.SMALL_FIELD_HEIGHT);
            context.drawText(this.textRenderer ,Text.literal("Coords: ").formatted(Formatting.GRAY), contentX + 4, (int)(yPos + (BuildScreenLayouts.SMALL_FIELD_HEIGHT - 8) / 2f + 1), Colors.TEXT_MUTED, false);

            int dimensionX = contentX + fieldWidth + BuildScreenLayouts.PANEL_SPACING;
            UIHelper.drawPanel(context, dimensionX, yPos, fieldWidth, BuildScreenLayouts.SMALL_FIELD_HEIGHT);
            context.drawText(this.textRenderer, Text.literal("Dimension: ").formatted(Formatting.GRAY), dimensionX + 4, (int)(yPos + (BuildScreenLayouts.SMALL_FIELD_HEIGHT - 8) / 2f + 1), Colors.TEXT_MUTED, false);
            yPos += BuildScreenLayouts.SMALL_FIELD_HEIGHT + BuildScreenLayouts.PANEL_SPACING;
            if (!build.getImageFileNames().isEmpty()) {
                int galleryBoxHeight = (int) (contentWidth * (BuildScreenLayouts.GALLERY_ASPECT_RATIO_H / BuildScreenLayouts.GALLERY_ASPECT_RATIO_W));
                UIHelper.drawPanel(context, contentX, yPos, contentWidth, galleryBoxHeight);

                String currentImageName = build.getImageFileNames().get(currentImageIndex);
                if (downloadingImages.contains(currentImageName)) {
                    context.drawCenteredTextWithShadow(textRenderer, Text.literal("Loading image...").formatted(Formatting.YELLOW), this.width / 2, yPos + galleryBoxHeight / 2 - 4, Colors.TEXT_PRIMARY);
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
                        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Error or missing image").formatted(Formatting.RED), this.width / 2, yPos + galleryBoxHeight / 2 - 4, Colors.TEXT_PRIMARY);
                    }
                }
                String counter = (currentImageIndex + 1) + " / " + build.getImageFileNames().size();
                int counterWidth = textRenderer.getWidth(counter);
                context.drawText(this.textRenderer, counter, contentX + contentWidth - counterWidth - 5, yPos + galleryBoxHeight - 12, Colors.TEXT_PRIMARY, false);

                yPos += galleryBoxHeight + BuildScreenLayouts.PANEL_SPACING;
            }

            // --- DYNAMIC CONTENT ---
            context.drawText(this.textRenderer, Text.literal("Description:").formatted(Formatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
            UIHelper.drawPanel(context, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT);
            yPos += BuildScreenLayouts.DESCRIPTION_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

            context.drawText(this.textRenderer, Text.literal("Credits:").formatted(Formatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
            UIHelper.drawPanel(context, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.CREDITS_FIELD_HEIGHT);
            yPos += BuildScreenLayouts.CREDITS_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;

            for (CustomField field : build.getCustomFields()) {
                context.drawText(this.textRenderer, Text.literal(field.getTitle() + ":").formatted(Formatting.GRAY), contentX, yPos, Colors.TEXT_PRIMARY, false);
                UIHelper.drawPanel(context, contentX, yPos + BuildScreenLayouts.LABEL_HEIGHT, contentWidth, BuildScreenLayouts.CUSTOM_FIELD_HEIGHT);
                yPos += BuildScreenLayouts.CUSTOM_FIELD_HEIGHT + BuildScreenLayouts.LABEL_HEIGHT + BuildScreenLayouts.PANEL_SPACING;
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
                    Identifier textureId = this.client.getTextureManager().registerDynamicTexture("buildnotes_image_" + build.getId() + "_" + fileName.hashCode(), texture);

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