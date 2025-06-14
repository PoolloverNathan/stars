package org.eu.net.pool.fabric.cots.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.eu.net.pool.fabric.cots.InventorySlotAccess;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;
    @Shadow public abstract ItemStack getStack(int slot);

    @WrapOperation(method = "getEmptySlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    boolean hideEmptyLockedSlots(ItemStack instance, Operation<Boolean> original, @Local int i) {
        return original.call(instance) && StarsKt.getInventorySlotAccessEvent().invoker().invoke(instance, player, i) == InventorySlotAccess.ALLOW;
    }

    @WrapOperation(method = "getOccupiedSlotWithRoomForStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;"))
    ItemStack hidePartiallyFullSlots(PlayerInventory instance, int slot, Operation<ItemStack> original) {
        var orig = original.call(instance, slot);
        return StarsKt.getInventorySlotAccessEvent().invoker().invoke(orig, player, slot) == InventorySlotAccess.ALLOW ? orig : ItemStack.EMPTY;
    }
    @WrapOperation(method = "getOccupiedSlotWithRoomForStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;"))
    Object hidePartiallyFullStacks(DefaultedList<ItemStack> instance, int slot, Operation<ItemStack> original) {
        var orig = original.call(instance, slot);
        return StarsKt.getInventorySlotAccessEvent().invoker().invoke(orig, player, slot) == InventorySlotAccess.ALLOW ? orig : ItemStack.EMPTY;
    }

    @Inject(method = "removeStack(I)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    void disallowRemoveLockedSlots(int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (StarsKt.getInventorySlotAccessEvent().invoker().invoke(getStack(slot), player, slot) != InventorySlotAccess.ALLOW) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    void injectInsertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot != -1 && StarsKt.getInventorySlotAccessEvent().invoker().invoke(getStack(slot), player, slot) != InventorySlotAccess.ALLOW) {
            cir.setReturnValue(false);
        }
    }
}
