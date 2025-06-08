package org.eu.net.pool.fabric.cots.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/screen/PlayerScreenHandler$1")
public class ArmorSlotMixin {
    @Shadow @Final EquipmentSlot field_7834;

    @ModifyExpressionValue(method = "canTakeItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isCreative()Z"))
    boolean modifyIsCreative(boolean original) {
        return false;
    }

    @ModifyReturnValue(method = "canTakeItems", at = @At("TAIL"))
    boolean canTakeItems(boolean original, PlayerEntity player) {
        return original && StarsKt.effectiveLevel(player, Enchantments.BINDING_CURSE, field_7834) <= 0;
    }
}
