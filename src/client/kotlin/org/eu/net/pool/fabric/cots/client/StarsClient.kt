package org.eu.net.pool.fabric.cots.client

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.advancement.criterion.OnKilledCriterion
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.data.server.advancement.vanilla.VanillaAdvancementProviders
import net.minecraft.item.Items
import net.minecraft.predicate.NumberRange
import net.minecraft.predicate.entity.EntityEquipmentPredicate
import net.minecraft.predicate.entity.EntityPredicate
import net.minecraft.predicate.item.EnchantmentPredicate
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.text.Text
import org.eu.net.pool.fabric.cots.SilenceCurse
import org.eu.net.pool.fabric.cots.StoneCurse
import org.eu.net.pool.fabric.cots.id
import org.eu.net.pool.fabric.cots.modid
import poollovernathan.fabric.DataContext
import java.util.function.Consumer

fun init() {

}

fun datagen(gen: FabricDataGenerator) {
    with(DataContext(gen)) {
        val gorgonSlayerTitle = "advancement.$modid.gorgon_slayer.title"
        val gorgonSlayerDescription = "advancement.$modid.gorgon_slayer.description"
        lang("en_us") {
            SilenceCurse.translation = "Curse of Silence"
            StoneCurse.translation = "Curse of Terra" // t3rracat reference??
            StoneCurse.StoneArmorMaterial.armorItems.forEach { (type, item) ->
                item.translation = "Stone ${type.name[0] + type.name.substring(1).lowercase()}"
            }
            gorgonSlayerTitle.translation = "Gorgon Slayer"
            gorgonSlayerDescription.translation = "Kill a player wearing a helmet enchanted with the Curse of Terra"
            StoneCurse.Petrified.PetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
            StoneCurse.Petrified.LongPetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
            StoneCurse.Petrified.PermPetrPotion.finishTranslationKey("$modid.").translation = "Petrification"
        }
        provider {
            object: FabricAdvancementProvider(it) {
                override fun generateAdvancement(p0: Consumer<Advancement>) {
                    Advancement.Builder.createUntelemetered()
                        .display(Items.STONE_SWORD, Text.translatable(gorgonSlayerTitle), Text.translatable(gorgonSlayerDescription), null, AdvancementFrame.CHALLENGE, /* showToast */ true, /* announceToChat */ true, /* hidden */ true)
                        .criterion("kill_gorgon", OnKilledCriterion.Conditions.createPlayerKilledEntity(
                            EntityPredicate.Builder.create()
                                .equipment(EntityEquipmentPredicate.Builder.create()
                                    .head(ItemPredicate.Builder.create()
                                        .enchantment(
                                            EnchantmentPredicate(StoneCurse, NumberRange.IntRange.ANY)
                                        ).build()
                                    ).build()
                                ).build()
                        ))
                        .build(p0, "gorgon")
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