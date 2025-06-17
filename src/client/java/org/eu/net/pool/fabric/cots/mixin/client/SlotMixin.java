package org.eu.net.pool.fabric.cots.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.eu.net.pool.fabric.cots.NoInventoryCurse;
import org.eu.net.pool.fabric.cots.StarsKt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Slot.class)
public class SlotMixin {
    @Shadow @Final public Inventory inventory;
    @Shadow @Final private int index;

    @Unique
    private int stars$getEffectiveSettlerLevel() {
        return inventory instanceof PlayerInventory pi ? StarsKt.effectiveLevel(pi.player, NoInventoryCurse.INSTANCE) : 0;
    }

    @Unique
    private boolean stars$doesSettlerAllowAccess() {
        return !switch (stars$getEffectiveSettlerLevel()) {
            case 0 -> false;
            case 1 -> index >= 1 && index < 36;
            default -> index >= 0 && index < 36;
        };
    }

    @WrapMethod(method = "isEnabled")
    boolean isEnabled(Operation<Boolean> orig) {
        return stars$doesSettlerAllowAccess() && orig.call();
    }

    @WrapMethod(method = "canInsert")
    boolean canInsert(ItemStack stack, Operation<Boolean> orig) {
        return stars$doesSettlerAllowAccess() && orig.call(stack);
    }

    @WrapMethod(method = {"canTakeItems", "canTakePartial"})
    boolean canTakeItems(PlayerEntity player, Operation<Boolean> orig) {
        return stars$doesSettlerAllowAccess() && orig.call(player);
    }
}
