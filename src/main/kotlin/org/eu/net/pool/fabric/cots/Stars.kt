@file:OptIn(ExperimentalContracts::class)

//region Metadata
package org.eu.net.pool.fabric.cots

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.*
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.ItemTags
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import poollovernathan.fabric.RegistryWrapper
import kotlin.contracts.ExperimentalContracts

const val modid = "stars"
val String.id get() = Identifier(modid, this)
//endregion
//region Helpers
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

abstract class Curse(rarity: Rarity = Rarity.RARE, target: EnchantmentTarget, vararg val slots: EquipmentSlot): Enchantment(rarity, target, slots) {
    constructor(rarity: Rarity = Rarity.RARE, vararg slots: EquipmentSlot): this(rarity, if (slots.size == 1) {
        when (slots[0]) {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND -> EnchantmentTarget.WEAPON
            EquipmentSlot.FEET -> EnchantmentTarget.ARMOR_FEET
            EquipmentSlot.LEGS -> EnchantmentTarget.ARMOR_LEGS
            EquipmentSlot.CHEST -> EnchantmentTarget.ARMOR_CHEST
            EquipmentSlot.HEAD -> EnchantmentTarget.ARMOR_HEAD
        }
    } else if (slots.all { it.type == EquipmentSlot.Type.ARMOR }) {
        EnchantmentTarget.ARMOR
    } else if (slots.all { it.type == EquipmentSlot.Type.HAND }) {
        EnchantmentTarget.WEAPON
    } else {
        EnchantmentTarget.BREAKABLE
    }, *slots)
    constructor(vararg slots: EquipmentSlot): this(Rarity.RARE, *slots)
    override fun isCursed() = true
    override fun isAcceptableItem(stack: ItemStack) =
        (stack.item.run { this is ArmorItem && slots.contains(slotType) }) || slots.any { it.type == EquipmentSlot.Type.HAND }
}
//endregion
//region Curses
// TODO: SVC compat
object SilenceCurse: Curse(EquipmentSlot.HEAD)
object StoneCurse: Curse(EquipmentSlot.CHEST) {
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
        is BlockItem if try { block.defaultState.isFullCube(null, null) } catch (_: NullPointerException) { false } -> Items.STONE
        else -> this
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
}
//endregion
//region Events
@JvmName("livingEntityTick")
fun LivingEntity.extraTick() {
    if (effectiveLevel(StoneCurse) >= 1) {
        for (slot in enumValues<EquipmentSlot>()) {
            getEquippedStack(slot).run a@{
                StoneCurse.run { item.stoneForm }.let {
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
//endregion
//region Chores
fun panic(msg: String): Nothing = with(Thread.currentThread()) {
    with(stackTrace[1]) {
        System.err.println("thread '${name}' panicked at $fileName:$lineNumber: $msg")
        Runtime.getRuntime().halt(100)
        checkNotNull<Nothing>(null)
    }
}

fun init() {
    with(RegistryWrapper.items) {
        StoneCurse.StoneArmorMaterial.armorItems.forEach { type, item -> item.register("stone_${type.name.lowercase()}".id) }
    }
    with(RegistryWrapper(Registries.ENCHANTMENT)) {
        SilenceCurse.register("silence".id)
        StoneCurse.register("stone".id)
    }
}
//endregion