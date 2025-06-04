package org.eu.net.pool.fabric.cots.client

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.advancement.criterion.OnKilledCriterion
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items
import net.minecraft.predicate.NumberRange
import net.minecraft.predicate.entity.EntityEquipmentPredicate
import net.minecraft.predicate.entity.EntityPredicate
import net.minecraft.predicate.item.EnchantmentPredicate
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.eu.net.pool.fabric.cots.LevitationCurse
import org.eu.net.pool.fabric.cots.SilenceCurse
import org.eu.net.pool.fabric.cots.StoneCurse
import org.eu.net.pool.fabric.cots.modid
import poollovernathan.fabric.DataContext
import java.util.function.Consumer

fun init() {

}

class AdvancementBuilder(val id: String, val builder: context(Advancement.Builder) AdvancementBuilder.() -> Unit) {
    val prefix = "advancement.$modid.$id"
    val titleKey = "$prefix.title"
    val titleText = Text.translatable(titleKey)!!
    val descriptionKey = "$prefix.description"
    val descriptionText = Text.translatable(descriptionKey)!!
    context(DataContext.Language)
    fun translate(title: String, description: String) {
        titleKey.translation = title
        descriptionKey.translation = description
    }
    fun build(c: Consumer<Advancement>) = Advancement.Builder.createUntelemetered().also { it.builder(this) }.build(c, id)
    fun Advancement.Builder.display(item: ItemConvertible, background: Identifier? = null, frame: AdvancementFrame = AdvancementFrame.TASK, showToast: Boolean = true, announceToChat: Boolean = true, hidden: Boolean = false)
        = display(item, titleText, descriptionText, background, frame, showToast, announceToChat, hidden)
    companion object {
        val gorgonSlayer = AdvancementBuilder("gorgon_slayer") {
            display(Items.STONE_SWORD, null, AdvancementFrame.CHALLENGE, hidden = true)
            criterion("kill_gorgon", OnKilledCriterion.Conditions.createPlayerKilledEntity(
                EntityPredicate.Builder.create()
                    .equipment(EntityEquipmentPredicate.Builder.create()
                        .head(ItemPredicate.Builder.create()
                            .enchantment(
                                EnchantmentPredicate(StoneCurse, NumberRange.IntRange.ANY)
                            ).build()
                        ).build()
                    ).build()
            ))
        }
    }
}

val Enchantment.descriptionKey get() = Registries.ENCHANTMENT.getId(this)?.run {
    if (namespace == "minecraft") {
        "enchantment.$path.desc"
    } else {
        "enchantment.$namespace.$path.desc"
    }
}

fun datagen(gen: FabricDataGenerator) {
    with(DataContext(gen)) {
        lang("en_us") {
            SilenceCurse.translation = "Curse of Silence"
            SilenceCurse.descriptionKey?.translation = "Wearer loses the ability to speak."
            StoneCurse.translation = "Curse of Terra" // t3rracat reference??
            StoneCurse.descriptionKey?.translation = "Items turn to stone."
            LevitationCurse.translation = "Curse of the Hanged Man"
            LevitationCurse.descriptionKey?.translation = "Levitate above the ground at all times."
            StoneCurse.StoneArmorMaterial.armorItems.forEach { (type, item) ->
                item.translation = "Stone ${type.name[0] + type.name.substring(1).lowercase()}"
            }
            AdvancementBuilder.gorgonSlayer.translate("Gorgon Slayer", "Kill a player wearing a helmet enchanted with the Curse of Terra")
            StoneCurse.Petrified.PetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
            StoneCurse.Petrified.LongPetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
            StoneCurse.Petrified.PermPetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
        }
        provider {
            object: FabricAdvancementProvider(it) {
                override fun generateAdvancement(p0: Consumer<Advancement>) {
                    AdvancementBuilder.gorgonSlayer.build(p0)
                }
            }
        }
        provider {
            object: FabricModelProvider(it) {
                override fun generateBlockStateModels(p0: BlockStateModelGenerator) {

                }
                override fun generateItemModels(p0: ItemModelGenerator) {
                    StoneCurse.StoneArmorMaterial.armorItems.values.forEach(p0::registerArmor)
                }
            }
        }
    }
}