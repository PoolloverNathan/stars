@file:OptIn(ExperimentalContracts::class)

package org.eu.net.pool.fabric.cots

import io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess
import net.minecraft.block.Blocks
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.*
import net.minecraft.item.trim.ArmorTrim
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import poollovernathan.fabric.RegistryWrapper
import sun.misc.Unsafe
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val modid = "stars"
val String.id get() = Identifier(modid, this)

interface EnchantmentDelegate {
    fun ItemStack.getLevel(enchantment: Enchantment, slot: EquipmentSlot): Int
}

fun LivingEntity.effectiveLevel(e: Enchantment, vararg slots: EquipmentSlot): Int {
    val slots = if (slots.isEmpty()) arrayOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD) else slots
    var n = 0
    slots.forEach {
        val stack = getEquippedStack(it)
        EnchantmentHelper.get(stack).forEach { (e2, lvl) ->
            if (e2 == e) n += lvl
            else if (e2 is EnchantmentDelegate) n += e2.run { stack.getLevel(e, it) }
        }
    }
    return n
}

// TODO: SVC compat
object SilenceCurse: Enchantment(Rarity.RARE, EnchantmentTarget.ARMOR_HEAD, arrayOf(EquipmentSlot.HEAD)) {
    override fun isCursed() = true
}

object StoneCurse: Enchantment(Rarity.RARE, EnchantmentTarget.ARMOR, arrayOf(EquipmentSlot.CHEST, EquipmentSlot.LEGS)) {
    override fun isCursed() = true
}

object StoneArmorMaterial: ArmorMaterial {
    override fun getDurability(type: ArmorItem.Type) = when (type) {
        ArmorItem.Type.HELMET -> 130
        ArmorItem.Type.CHESTPLATE -> 150
        ArmorItem.Type.LEGGINGS -> 160
        ArmorItem.Type.BOOTS -> 110
    }
    override fun getProtection(type: ArmorItem.Type) = ArmorMaterials.CHAIN.getProtection(type)
    override fun getEnchantability() = 5
    override fun getEquipSound() = SoundEvents.ITEM_ARMOR_EQUIP_GENERIC!!
    override fun getRepairIngredient(): Ingredient = Ingredient.fromTag(ItemTags.STONE_TOOL_MATERIALS)
    override fun getName() = "stars:stone"
    override fun getToughness() = 0f
    override fun getKnockbackResistance() = 0f

    // TODO: horse armor
    val armorItems: Map<ArmorItem.Type, ArmorItem> = enumValues<ArmorItem.Type>().asIterable().associateWith {
        object: ArmorItem(StoneArmorMaterial, it, Settings().maxDamage(getDurability(it))) {}
    }
}

@JvmName("livingEntityTick")
fun LivingEntity.extraTick() {
    if (effectiveLevel(StoneCurse) >= 1) {
        for (slot in enumValues<EquipmentSlot>()) {
            getEquippedStack(slot).run a@{
                item.stoneForm.let {
                    if (it == item) return@a
                    if (isDamaged) { damage = damage * it.maxDamage / item.maxDamage }
                    // TODO: armor tf animation
                    equipStack(slot, ItemStack(it, count).also { it.nbt = nbt })
                }
            }
        }
        // TODO: carryon compat >:3
    }
}

val Item.stoneForm: Item get() = when (this) {
    is AxeItem -> Items.STONE_AXE
    is HoeItem -> Items.STONE_HOE
    is PickaxeItem -> Items.STONE_PICKAXE
    is ShovelItem -> Items.STONE_SHOVEL
    is SwordItem -> Items.STONE_SWORD
    is ToolItem -> panic("Unsupported tool class ${javaClass.name}")
    // TODO: preserve leather dye
    is ArmorItem -> checkNotNull(StoneArmorMaterial.armorItems[type])
    // TODO: prevent cobblestone etc. conversion
    is BlockItem if try { block.defaultState.isFullCube(null, null) } catch (e: NullPointerException) { false } -> Items.STONE
    else -> this
}

fun panic(msg: String): Nothing = with(Thread.currentThread()) {
    with(stackTrace[1]) {
        System.err.println("thread '${name}' panicked at $fileName:$lineNumber: $msg")
        Runtime.getRuntime().halt(100)
        checkNotNull<Nothing>(null)
    }
}

fun init() {
    with(RegistryWrapper.items) {
        StoneArmorMaterial.armorItems.forEach { type, item -> item.register("stone_${type.getName()}".id) }
    }
    with(RegistryWrapper(Registries.ENCHANTMENT)) {
        SilenceCurse.register("silence".id)
        StoneCurse.register("stone".id)
    }
}