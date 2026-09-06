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
    @Inject(method = "handleChatInput", at= @At("HEAD"), cancellable = true)
    public void handleChatInput(String msg, final boolean addToRecent, CallbackInfo ci) {
        if (!msg.isEmpty()) {
            String lowerString = msg.toLowerCase();
            if (lowerString.startsWith("@miq")) {
                String messageString = LastMessageTemp.LastMessage.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
                String logTag = (String) Optionull.map(LastMessageTemp.LastMessage.tag(), GuiMessageTag::logTag);

                CompletableFuture<Path> miqPath = new FetchMiQ().fetchMiQ(messageString);
                try {
                    Path path = miqPath.get();

                    Minecraft.getInstance().setScreen(new MiQScreen(path));
                    ci.cancel();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}