package org.shark.miq.client.mixin;

import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.shark.miq.client.FetchMiQ;
import org.shark.miq.client.LastMessageTemp;
import org.shark.miq.client.MiQScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Mixin(ChatScreen.class)
public class ChatMenu {

    @Inject(
            method = "handleChatInput",
            at = @At("HEAD"),
            cancellable = true
    )
    public void handleChatInput(
            String msg,
            final boolean addToRecent,
            CallbackInfo ci
    ) {
        if (msg.isEmpty()) {
            return;
        }

        String lowerString = msg.toLowerCase();

        if (!lowerString.startsWith("@miq")) {
            return;
        }

        String messageString = LastMessageTemp.LastMessage
                .content()
                .getString()
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        String authorName = messageString.split(">")[0].split("<")[1];

        CompletableFuture<Path> future =
                new FetchMiQ().fetchMiQ(messageString, authorName);

        future.thenAccept(path -> {
            Minecraft.getInstance().execute(() -> {
                System.out.println("Opening MiQScreen: " + path);

                Minecraft.getInstance().setScreen(
                        new MiQScreen(path)
                );
            });
        }).exceptionally(error -> {
            error.printStackTrace();
            return null;
        });

        ci.cancel();
    }
}