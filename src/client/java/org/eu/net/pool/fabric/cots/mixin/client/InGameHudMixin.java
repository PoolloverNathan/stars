package org.eu.net.pool.fabric.cots.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.eu.net.pool.fabric.cots.NoInventoryCurse;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V", ordinal = 0))
    void filterHotbar(InGameHud instance, DrawContext context, int x, int y, float f, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original, @Local(index = 10) int m, @Share("lvl") LocalIntRef lvlRef) {
        int lvl = lvlRef.get();
        switch (lvlRef.get()) {
            case 0 -> original.call(instance, context, x, y, f, player, stack, seed);
            case 1 -> {
                if (m == 0) original.call(instance, context, x + 80, y, f, player, stack, seed);
            }
        }
    }

    @WrapOperation(method = "renderHotbar", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V", ordinal = 1),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IIFLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V", ordinal = 2)
    })
    void shiftOffhand(InGameHud instance, DrawContext context, int x, int y, float f, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original, @Share("lvl") LocalIntRef lvlRef, @Local Arm arm) {
        original.call(instance, context, lvlRef.get() >= 1 ? arm == Arm.LEFT ? x + 80 : x - 80 : x, y, f, player, stack, seed);
    }

    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V", ordinal = 0))
    void modifyHotbarRendering(DrawContext instance, Identifier texture, int x, int y, int u, int v, int width, int height, Operation<Void> original, @Local PlayerEntity player, @Share("lvl") LocalIntRef lvlRef) {
        int lvl = StarsKt.effectiveLevel(player, NoInventoryCurse.INSTANCE);
        lvlRef.set(lvl);
        switch (lvl) {
            case 0 -> original.call(instance, texture, x, y, u, v, width, height);
            case 1 -> original.call(instance, texture, x + 80, y, u, v, 20, height);
        }
    }

    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V", ordinal = 1))
    void modifyHotbarSelector(DrawContext instance, Identifier texture, int x, int y, int u, int v, int width, int height, Operation<Void> original, @Local PlayerEntity player, @Share("lvl") LocalIntRef lvlRef) {
        switch (lvlRef.get()) {
            case 0 -> original.call(instance, texture, x, y, u, v, width, height + 1);
            case 1 -> original.call(instance, texture, x + 80, y, u, v, width, height + 1);
        }
    }

    @WrapOperation(method = "renderHotbar", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V", ordinal = 2),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V", ordinal = 3)
    })
    void shiftOffhand(DrawContext instance, Identifier texture, int x, int y, int u, int v, int width, int height, Operation<Void> original, @Share("lvl") LocalIntRef lvlRef, @Local Arm arm) {
        original.call(instance, texture, lvlRef.get() >= 1 ? arm == Arm.LEFT ? x + 80 : x - 80 : x, y, u, v, width, height);
    }
}
