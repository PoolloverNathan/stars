package org.eu.net.pool.fabric.cots

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import poollovernathan.fabric.RegistryWrapper

val modid = "stars"
val String.id get() = Identifier(modid, this)

interface EnchantmentDelegate {
    fun ItemStack.getLevel(enchantment: Enchantment): Int
}

fun LivingEntity.effectiveLevel(e: Enchantment, vararg slots: EquipmentSlot): Int {
    val slots = if (slots.isEmpty()) arrayOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD) else slots
    var n = 0
    slots.forEach {
        val stack = getEquippedStack(it)
        EnchantmentHelper.get(stack).forEach { (e2, lvl) ->
            if (e2 == e) n += lvl
            else if (e2 is EnchantmentDelegate) n += e2.run { stack.getLevel(e) }
        }
    }
    return n
}

// TODO: SVC compat
object SilenceCurse: Enchantment(Rarity.RARE, EnchantmentTarget.ARMOR_HEAD, arrayOf(EquipmentSlot.HEAD)) {
    override fun isCursed() = true
}

fun init() {
    with(RegistryWrapper(Registries.ENCHANTMENT)) {
        SilenceCurse.register("silence".id)
    }
}