package org.eu.net.pool.fabric.cots.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.eu.net.pool.fabric.cots.SilenceCurse;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    void stars$preSendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {;
        if (isActive()) cir.cancel();
    }

    private boolean isActive() {
        var player = MinecraftClient.getInstance().player;
        return player != null && StarsKt.effectiveLevel(player, SilenceCurse.INSTANCE) >= 1;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    void stars$fillProxy(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
        var player = MinecraftClient.getInstance().player;
        if (!isActive()) instance.fill(x1, y1, x2, y2, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    void stars$renderProxy(TextFieldWidget instance, DrawContext drawContext, int i, int j, float v) {
        var player = MinecraftClient.getInstance().player;
        if (!isActive()) instance.render(drawContext, i, j, v);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;render(Lnet/minecraft/client/gui/DrawContext;II)V"))
    void stars$renderProxy2(ChatInputSuggestor instance, DrawContext context, int mouseX, int mouseY) {
        var player = MinecraftClient.getInstance().player;
        if (!isActive()) instance.render(context, mouseX, mouseY);
    }
}
