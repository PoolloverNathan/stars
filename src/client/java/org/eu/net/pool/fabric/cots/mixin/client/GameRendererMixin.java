package org.eu.net.pool.fabric.cots.mixin.client;

import net.minecraft.client.render.GameRenderer;
import org.eu.net.pool.fabric.cots.client.StarsClientKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", ordinal = 0))
    private void afterHud(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        StarsClientKt.renderOverlays((GameRenderer) (Object) this);
    }
}
