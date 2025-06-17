package org.eu.net.pool.fabric.cots.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.eu.net.pool.fabric.cots.InventorySlotAccess;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Slot.class)
public abstract class SlotMixin {
    @Shadow @Final public Inventory inventory;
    @Shadow @Final private int index;
    @Shadow public abstract ItemStack getStack();

    @Unique
    private InventorySlotAccess stars$accessType() {
        return inventory instanceof PlayerInventory pi ? StarsKt.getInventorySlotAccessEvent().invoker().invoke(getStack(), pi.player, index) : InventorySlotAccess.ALLOW;
    }

    @WrapMethod(method = "isEnabled")
    boolean isEnabled(Operation<Boolean> orig) {
        return stars$accessType() != InventorySlotAccess.LOCK_AND_DROP && orig.call();
    }

    @WrapMethod(method = "canInsert")
    boolean canInsert(ItemStack stack, Operation<Boolean> orig) {
        return stars$accessType() == InventorySlotAccess.ALLOW && orig.call(stack);
    }

    @WrapMethod(method = {"canTakeItems", "canTakePartial"})
    boolean canTakeItems(PlayerEntity player, Operation<Boolean> orig) {
        return stars$accessType() == InventorySlotAccess.ALLOW && orig.call(player);
    }
}
