package org.shark.miq.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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
    Path miqImagePath;

    private DynamicTexture texture;
    private Identifier textureId;

    public MiQScreen(Path imagePath) {
        super(Component.literal("MiQ Viewer"));

        miqImagePath = imagePath;
    }

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");

    @Override
    protected void init() {
        try {
            String fileName = LocalDateTime.now().format(FORMATTER);

            NativeImage image = NativeImage.read(Files.newInputStream(miqImagePath));

            texture = new DynamicTexture(
                    () -> "miq_image_" + fileName,
                    image
            );

            Identifier indClass = Identifier.parse("miq_image_" + fileName);

            Minecraft.getInstance()
                    .getTextureManager()
                    .register(
                            indClass,
                            texture
                    );

            textureId = indClass;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (textureId == null || texture == null) {
            return;
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                50,
                50,
                0,
                0,
                256,
                256,
                texture.getPixels().getWidth(),
                texture.getPixels().getHeight()
        );
    }

    @Override
    public void onClose() {
        if (textureId != null) {
            Minecraft.getInstance()
                    .getTextureManager()
                    .release(textureId);
        }

        super.onClose();
    }
}
