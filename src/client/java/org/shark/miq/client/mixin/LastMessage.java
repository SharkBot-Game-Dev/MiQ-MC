package org.shark.miq.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.shark.miq.client.LastMessageTemp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class LastMessage {
    @Inject(method = "logChatMessage", at= @At("HEAD"))
    public void logChatMessage(final GuiMessage message, CallbackInfo ci) {
        LastMessageTemp.LastMessage = message;
    }
}
