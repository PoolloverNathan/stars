package org.eu.net.pool.fabric.cots.mixin;

import net.minecraft.entity.LivingEntity;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    void postTick(CallbackInfo ci) {
        StarsKt.livingEntityTick((LivingEntity) (Object) this);
    }
}
