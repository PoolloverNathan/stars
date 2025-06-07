@file:OptIn(ExperimentalContracts::class)

//region Metadata
package org.eu.net.pool.fabric.cots

import dev.onyxstudios.cca.api.v3.component.Component
import dev.onyxstudios.cca.api.v3.component.ComponentKey
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.block.*
import net.minecraft.command.argument.RegistryKeyArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.potion.Potion
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.ItemTags
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.eu.net.pool.fabric.cots.StoneCurse.stoneForm
import poollovernathan.fabric.*
import kotlin.contracts.ExperimentalContracts
import kotlin.math.nextDown

const val modid = "stars"
val String.id get() = Identifier(modid, this)
//endregion
//region Helpers

interface EnchantmentDelegate {
    fun ItemStack.getLevel(enchantment: Enchantment, slot: EquipmentSlot): Int
}

fun LivingEntity.effectiveLevel(e: Enchantment, vararg slots: EquipmentSlot) =
    (slots.takeUnless { it.isEmpty() } ?: arrayOf(
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    )).let {
        it.sumOf {
            val stack = getEquippedStack(it)
            EnchantmentHelper.get(stack).entries.sumOf { (e2, lvl) ->
                if (e2 == e)
                    lvl
                else if (e2 is EnchantmentDelegate)
                    e2.run { stack.getLevel(e, it) }
                else 0
            }
        } + it.maxOf {
            innateCurses.curses.getOrDefault(e to it, 0)
        }
    }

val LivingEntity.isSilenced @JvmName("isSilenced") get() = effectiveLevel(SilenceCurse) >= 1 || hasStatusEffect(StoneCurse.Petrified)

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
object LevitationCurse: Curse(EquipmentSlot.FEET) {
    override fun getMaxLevel() = 3
}
object StoneCurse: Curse(EquipmentSlot.HEAD, EquipmentSlot.CHEST) {
    const val PETRIFY_TIME = 5*60*20
    object Petrified: StatusEffect(StatusEffectCategory.HARMFUL, 0x696764) {
        // TODO: give petrification more effects than simply silencing
        object PetrPotion: Potion("petrify", StatusEffectInstance(Petrified, PETRIFY_TIME))
        object LongPetrPotion: Potion("petrify_long", StatusEffectInstance(Petrified, PETRIFY_TIME*2))
        object PermPetrPotion: Potion("petrify_permanent", StatusEffectInstance(Petrified, -1))
    }
    val Item.stoneForm: Item get() = when (this) {
        is AxeItem -> Items.STONE_AXE
        is HoeItem -> Items.STONE_HOE
        is PickaxeItem -> Items.STONE_PICKAXE
        is ShovelItem -> Items.STONE_SHOVEL
        is SwordItem -> Items.STONE_SWORD
        is ToolItem -> {
            System.err.println("Unsupported tool class ${javaClass.name}")
            this
        }
        // TODO: preserve leather dye
        is ArmorItem -> checkNotNull(StoneArmorMaterial.armorItems[type])
        is BlockItem -> block.stoneForm.asItem()
        else -> this
    }
    val Block.stoneForm: Block get() = when (this) {
        Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR -> this
        Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK, Blocks.GRAVEL -> Blocks.GRAVEL
        Blocks.COBBLESTONE -> Blocks.COBBLESTONE
        Blocks.SAND, Blocks.RED_SAND -> this
        Blocks.PETRIFIED_OAK_SLAB -> Blocks.PETRIFIED_OAK_SLAB
        Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE -> this
        is PlantBlock -> Blocks.AIR
        // ez concrete hardening
        Blocks.WHITE_WOOL,      Blocks.WHITE_CONCRETE_POWDER      -> Blocks.WHITE_CONCRETE
        Blocks.ORANGE_WOOL,     Blocks.ORANGE_CONCRETE_POWDER     -> Blocks.ORANGE_CONCRETE
        Blocks.MAGENTA_WOOL,    Blocks.MAGENTA_CONCRETE_POWDER    -> Blocks.MAGENTA_CONCRETE
        Blocks.LIGHT_BLUE_WOOL, Blocks.LIGHT_BLUE_CONCRETE_POWDER -> Blocks.LIGHT_BLUE_CONCRETE
        Blocks.YELLOW_WOOL,     Blocks.YELLOW_CONCRETE_POWDER     -> Blocks.YELLOW_CONCRETE
        Blocks.LIME_WOOL,       Blocks.LIME_CONCRETE_POWDER       -> Blocks.LIME_CONCRETE
        Blocks.PINK_WOOL,       Blocks.PINK_CONCRETE_POWDER       -> Blocks.PINK_CONCRETE
        Blocks.GRAY_WOOL,       Blocks.GRAY_CONCRETE_POWDER       -> Blocks.GRAY_CONCRETE
        Blocks.LIGHT_GRAY_WOOL, Blocks.LIGHT_GRAY_CONCRETE_POWDER -> Blocks.LIGHT_GRAY_CONCRETE
        Blocks.CYAN_WOOL,       Blocks.CYAN_CONCRETE_POWDER       -> Blocks.CYAN_CONCRETE
        Blocks.PURPLE_WOOL,     Blocks.PURPLE_CONCRETE_POWDER     -> Blocks.PURPLE_CONCRETE
        Blocks.BLUE_WOOL,       Blocks.BLUE_CONCRETE_POWDER       -> Blocks.BLUE_CONCRETE
        Blocks.BROWN_WOOL,      Blocks.BROWN_CONCRETE_POWDER      -> Blocks.BROWN_CONCRETE
        Blocks.GREEN_WOOL,      Blocks.GREEN_CONCRETE_POWDER      -> Blocks.GREEN_CONCRETE
        Blocks.RED_WOOL,        Blocks.RED_CONCRETE_POWDER        -> Blocks.RED_CONCRETE
        Blocks.BLACK_WOOL,      Blocks.BLACK_CONCRETE_POWDER      -> Blocks.BLACK_CONCRETE
        is PressurePlateBlock, is CarpetBlock -> Blocks.STONE_PRESSURE_PLATE
        is SlabBlock -> Blocks.STONE_SLAB
        is StairsBlock -> Blocks.STONE_STAIRS
        else -> Blocks.STONE
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
        override fun getName() = "$modid:stone"
        override fun getToughness() = 0f
        override fun getKnockbackResistance() = 0f

        // TODO: horse armor
        val armorItems: Map<ArmorItem.Type, ArmorItem> = enumValues<ArmorItem.Type>().asIterable().associateWith {
            object: ArmorItem(StoneArmorMaterial, it, Settings().maxDamage(getDurability(it))) {}
        }
    }
}
object SunCurse: Curse(
    // blind + glow + emit light
    EquipmentSlot.HEAD,
    // glow + emit light
    EquipmentSlot.CHEST,
    EquipmentSlot.LEGS,
    EquipmentSlot.FEET,
    // emit light
    EquipmentSlot.MAINHAND,
    EquipmentSlot.OFFHAND,
) {
    // TODO:
    //  increase mob detection range
    //  blind viewers as well?
}
//endregion
//region Events

@JvmName("livingEntityTick")
fun LivingEntity.extraTick() {
    if (effectiveLevel(StoneCurse, EquipmentSlot.CHEST) >= 1) {
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
    effectiveLevel(LevitationCurse, EquipmentSlot.FEET).let { lvl ->
        if (lvl >= 1) {
            val targetHeight = lvl * 0.5 + 0.33
            val currentHeight: Double = with(boundingBox) {
                // TODO: see how this fares with Pehkui
                iterRange(minX..maxX.nextDown(), 0.25).minOf { x: Double ->
                    iterRange(minZ..maxZ.nextDown(), 0.25).minOf { z: Double ->
                        with(world.raycast(RaycastContext(Vec3d(x, minY, z), Vec3d(x, minY - targetHeight * 2, z), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, the<LivingEntity>()))) {
                            when (type) {
                                HitResult.Type.MISS -> targetHeight * 2
                                HitResult.Type.BLOCK -> y - pos.y
                                else -> throw AssertionError("I read the code and you should not be here.")
                            }
                        }
                    }
                }
            }
            if (this is PlayerEntity && abilities.flying) return@let
            addVelocity(0.0, (targetHeight - currentHeight) / 5 - velocity.y / 3, 0.0)
        }
    }
    if (effectiveLevel(StoneCurse, EquipmentSlot.HEAD) >= 1) {
        // TODO: figure out why this raycast is jank
        with(raycast(8.0, 0.0f, false)) {
            when (this) {
                is BlockHitResult -> with (blockStateAtPos) {
                    val stone = block.stoneForm
                    if (block != stone) {
                        world.setBlockState(blockPos, blockStateAtPos.properties.fold(block.stoneForm.defaultState) { state, prop -> Shim.setPropertyUnchecked(state, prop, blockStateAtPos[prop]) })
                    }
                }
                is EntityHitResult -> with (entity) {
                    val si = getStatusEffect(StoneCurse.Petrified)
                    if (si == null) {
                        world.playSound(null, blockPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS)
                        world.addBlockBreakParticles(blockPos, Blocks.STONE.defaultState)
                    } else if (si.duration < StoneCurse.PETRIFY_TIME) {
                        removeStatusEffect(StoneCurse.Petrified)
                    } else return@with
                    addStatusEffect(StoneCurse.Petrified.PetrPotion.effects[0])
                }
            }
        }
    }
}
//endregion
//region Chores

fun <T> T.the() = this

fun iterRange(range: ClosedRange<Double>, step: Double = 1.0) = sequence {
    var n = range.start
    while (n < range.endInclusive) {
        yield(n)
        n += step
    }
    yield(range.endInclusive)
}
fun iterRange(range: ClosedRange<Int>, step: Int = 1) = sequence {
    var n = range.start
    while (n < range.endInclusive) {
        yield(n)
        n += step
    }
    yield(range.endInclusive)
}
fun iterRange(range: OpenEndRange<Double>, step: Double = 1.0) = sequence {
    var n = range.start
    while (n < range.endExclusive) {
        yield(n)
        n += step
    }
}
fun iterRange(range: OpenEndRange<Int>, step: Int = 1) = sequence {
    var n = range.start
    while (n < range.endExclusive) {
        yield(n)
        n += step
    }
}

data class InnateCurseComponent(val entity: Entity, val curses: MutableMap<Pair<Enchantment, EquipmentSlot>, Int> = mutableMapOf()): Component, AutoSyncedComponent {
    override fun readFromNbt(nbt: NbtCompound) {
        nbt.getList("InnateCurses", NbtElement.COMPOUND_TYPE.toInt()).filterIsInstance<NbtCompound>().each {
            curses.put((getString("Enchantment").takeIf { it != "" }?.let(Identifier::tryParse)?.let(Registries.ENCHANTMENT::get) ?: return@each) to (getInt("Slot").let(EquipmentSlot.entries::getOrNull) ?: return@each), getInt("Level"))
        }
    }

    override fun writeToNbt(nbt: NbtCompound) {
        nbt.put("InnateCurses", NbtList().apply {
            curses.entries.each {
                add(NbtCompound().apply {
                    putString("Enchantment", (Registries.ENCHANTMENT.getId(key.first) ?: return@each).toString())
                    putInt("Slot", key.second.ordinal)
                    putInt("Level", value)
                })
            }
        })
    }
}

val LivingEntity.innateCurses: InnateCurseComponent get() = getComponent(innateCurseKey)

val innateCurseKey: ComponentKey<InnateCurseComponent> = ComponentRegistry.getOrCreate("innate_curses".id, InnateCurseComponent::class.java)

// TODO: more component factory types (chunk, world, level)
fun entityComponents(factory: EntityComponentFactoryRegistry) {
    factory.registerFor(LivingEntity::class.java, innateCurseKey, ::InnateCurseComponent)
    factory.registerForPlayers(innateCurseKey, ::InnateCurseComponent, RespawnCopyStrategy.CHARACTER)
}

object TranslationKeys {
    fun innateCurse(settingSlot: Boolean?, value: Boolean) =
        "text.stars.innate_curse.${if (settingSlot == null) { "get.$value" } else { "set.$value" + if (settingSlot) { ".slot" } else { "" }}}"
}

fun init() {
    with(RegistryWrapper.items) {
        StoneCurse.StoneArmorMaterial.armorItems.forEach { type, item -> item.register("stone_${type.name.lowercase()}".id) }
    }
    with(RegistryWrapper(Registries.ENCHANTMENT)) {
        SilenceCurse.register("silence".id)
        StoneCurse.register("stone".id)
        LevitationCurse.register("levitation".id)
        SunCurse.register("sun".id)
    }
    with(RegistryWrapper(Registries.STATUS_EFFECT)) {
        StoneCurse.Petrified.register("petrified".id)
    }
    with(RegistryWrapper(Registries.POTION)) {
        StoneCurse.Petrified.PetrPotion.register("petrify".id)
        StoneCurse.Petrified.LongPetrPotion.register("petrify_long".id)
        StoneCurse.Petrified.PermPetrPotion.register("petrify_permanent".id)
    }

    CommandRegistrationCallback.EVENT.register { /* doctor */ d, r, e ->
        Shim.makeContext(d).run {
            "stars" {
                "innateCurses" {
                    fun CommandContext<ServerCommandSource>.curseSlotSection(body: (CommandContext.CommandExecution<ServerCommandSource>.() -> Pair<Enchantment, EquipmentSlot>) -> Unit) {
                        arg("curse", RegistryKeyArgumentType.registryKey(RegistryKeys.ENCHANTMENT)) { curse ->
                            for (slot in EquipmentSlot.entries) {
                                slot.name.lowercase().invoke {
                                    body {
                                        Registries.ENCHANTMENT[this.curse()]!! to slot
                                    }
                                }
                            }
                        }
                    }

                    fun CommandContext<ServerCommandSource>.curseMaybeSlotSection(body: (CommandContext.CommandExecution<ServerCommandSource>.() -> Pair<Enchantment, EquipmentSlot?>) -> Unit) {
                        arg("curse", RegistryKeyArgumentType.registryKey(RegistryKeys.ENCHANTMENT)) { curse ->
                            body {
                                Registries.ENCHANTMENT[this.curse()]!! to null
                            }
                            for (slot in EquipmentSlot.entries) {
                                slot.name.lowercase().invoke {
                                    body {
                                        Registries.ENCHANTMENT[this.curse()]!! to slot
                                    }
                                }
                            }
                        }
                    }

                    fun CommandContext<ServerCommandSource>.section(
                        getter: CommandContext.CommandExecution<ServerCommandSource>.(Pair<Enchantment, EquipmentSlot>) -> Boolean,
                        setter: CommandContext.CommandExecution<ServerCommandSource>.(Enchantment, EquipmentSlot, Int) -> Unit
                    ) {
                        // TODO: feedback
                        "get" {
                            curseSlotSection { cs ->
                                runs {
                                    cs()
                                }
                            }
                        }
                        "set" {
                            curseMaybeSlotSection { cs ->
                                intArg("level", 0) { level ->
                                    runs {
                                        val (ench, slot) = cs()
                                        if (slot == null) {
                                            for (slot in EquipmentSlot.entries) {
                                                setter(ench, slot, level())
                                            }
                                        } else {
                                            setter(ench, slot, level())
                                        }
                                    }
                                }
                            }
                        }
                    }
                    playerArg { target ->
                        section({
                            target().innateCurses.curses.getOrDefault(it, 0) > 0
                        }) { ench, slot, lvl ->
                            target().innateCurses.run {
                                if (lvl <= 0)
                                    curses.remove(ench to slot)
                                else
                                    curses[ench to slot] = lvl
                            }
                            innateCurseKey.sync(target())
                        }
                    }
                }
            }
        }
    }
}
//endregion