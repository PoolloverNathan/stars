package org.eu.net.pool.fabric.cots.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/screen/PlayerScreenHandler$1")
public class ArmorSlotMixin {
    @ModifyExpressionValue(method = "canTakeItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isCreative()Z"))
    boolean modifyIsCreative(boolean original) {
        return false;
    }
}
