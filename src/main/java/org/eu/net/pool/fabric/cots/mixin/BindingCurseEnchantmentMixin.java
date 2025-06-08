package org.eu.net.pool.fabric.cots.mixin;

import net.minecraft.enchantment.BindingCurseEnchantment;
import org.eu.net.pool.fabric.cots.InnateCurseCompatible;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BindingCurseEnchantment.class)
public class BindingCurseEnchantmentMixin implements InnateCurseCompatible {
}
