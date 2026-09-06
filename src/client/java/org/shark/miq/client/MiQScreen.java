package org.shark.miq.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MiQScreen extends Screen {

    private final Path miqImagePath;

    private DynamicTexture texture;
    private Identifier textureId;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    public MiQScreen(Path imagePath) {
        super(Component.literal("MiQ Viewer"));
        this.miqImagePath = imagePath;
    }

    @Override
    protected void init() {
        super.init();

        try {
            String fileName = LocalDateTime.now().format(FORMATTER);

            NativeImage image;

            try (var input = Files.newInputStream(miqImagePath)) {
                image = NativeImage.read(input);
            }

            System.out.println("Loading image: " + miqImagePath);
            System.out.println("Image size: "
                    + image.getWidth() + "x" + image.getHeight());

            texture = new DynamicTexture(
                    () -> "miq_image_" + fileName,
                    image
            );

            textureId = Identifier.fromNamespaceAndPath(
                    "miq",
                    "image/" + fileName
            );

            Minecraft.getInstance()
                    .getTextureManager()
                    .register(textureId, texture);

            System.out.println("Texture registered: " + textureId);

        } catch (IOException e) {
            e.printStackTrace();
        }

        Button closeButton = Button.builder(Component.literal("Close"), (btn) -> {
            this.minecraft.setScreen(null);
        }).bounds(10, 210, 120, 20).build();

        this.addRenderableWidget(closeButton);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        if (textureId == null) {
            return;
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                10,
                30,
                0.0f,
                0.0f,
                300,
                150,
                texture.getPixels().getWidth(),
                texture.getPixels().getHeight()
        );

        graphics.text(this.font, "Done!", 10, 10, 0xFFFFFFFF, true);
    }

    @Override
    public void onClose() {
        if (textureId != null) {
            Minecraft.getInstance()
                    .getTextureManager()
                    .release(textureId);

            textureId = null;
            texture = null;
        }

        super.onClose();
    }
}