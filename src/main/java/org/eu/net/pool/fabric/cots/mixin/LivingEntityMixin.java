package org.eu.net.pool.fabric.cots.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.eu.net.pool.fabric.cots.SunCurse;
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

    @ModifyReturnValue(method = "isGlowing", at = @At("TAIL"))
    boolean modifyGlowing(boolean original) {
        return original || StarsKt.effectiveLevel((LivingEntity) (Object) this, SunCurse.INSTANCE, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET) >= 1;
    }
}
