package org.shark.miq.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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

        if (LastMessageTemp.LastMessage == null) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.literal("There are no images suitable for turning into a quote."));
            }
            ci.cancel();
            return;
        }

        boolean isColor = !msg.contains("gray");

        String messageString = LastMessageTemp.LastMessage
                .content()
                .getString()
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        String authorName = messageString.split(">")[0].split("<")[1];

        String prefix = "<" + authorName + ">";
        String removedAuthorNameContent = messageString.startsWith(prefix) ? messageString.substring(prefix.length()) : messageString;

        CompletableFuture<Path> future =
                new FetchMiQ().fetchMiQ(removedAuthorNameContent, authorName, isColor);

        future.thenAccept(path -> {
            Minecraft.getInstance().execute(() -> {
                MiQScreen screen = new MiQScreen(path, removedAuthorNameContent);
                screen.MiQUserName = authorName;

                Minecraft.getInstance().gui.setScreen(
                        screen
                );
            });
        }).exceptionally(error -> {
            error.printStackTrace();
            return null;
        });

        ci.cancel();
    }
}