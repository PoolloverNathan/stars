package org.eu.net.pool.fabric.cots.client

import com.mojang.blaze3d.systems.RenderSystem
import dev.lambdaurora.lambdynlights.api.data.ItemLightSourceDataProvider
import dev.lambdaurora.lambdynlights.api.item.ItemLuminance
import dev.lambdaurora.lambdynlights.api.predicate.ItemPredicate as LambItemPredicate
import kotlinx.coroutines.future.asCompletableFuture
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.advancement.Advancement
import net.minecraft.advancement.AdvancementFrame
import net.minecraft.advancement.criterion.OnKilledCriterion
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import net.minecraft.enchantment.Enchantment
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items
import net.minecraft.predicate.NbtPredicate
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
import org.eu.net.pool.fabric.cots.SunCurse
import org.eu.net.pool.fabric.cots.effectiveLevel
import org.eu.net.pool.fabric.cots.id
import org.eu.net.pool.fabric.cots.modid
import poollovernathan.fabric.DataContext
import java.util.Optional
import java.util.function.Consumer

fun init() {

}

fun GameRenderer.renderOverlays() {
    val player = MinecraftClient.getInstance().player ?: return

    if (player.effectiveLevel(SunCurse, EquipmentSlot.HEAD) >= 1) {
        val window = MinecraftClient.getInstance().window
        var width: Double = window.scaledWidth.toDouble()
        var height: Double = window.scaledHeight.toDouble()

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShader(GameRenderer::getPositionProgram)
        // RenderSystem.setShaderTexture(0, texture)
        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer;
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(0.0, height, -90.0).next();
        bufferBuilder.vertex(width, height, -90.0).next();
        bufferBuilder.vertex(width, 0.0, -90.0).next();
        bufferBuilder.vertex(0.0, 0.0, -90.0).next();
        tessellator.draw();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
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
                EntityPredicate.Builder.create().equipment(EntityEquipmentPredicate.Builder.create().head(enchantedWith(StoneCurse)).build()).build()
            ))
        }
    }
}

fun enchantedWith(enchantment: Enchantment, range: NumberRange.IntRange = NumberRange.IntRange.ANY) = ItemPredicate.Builder.create().enchantment(EnchantmentPredicate(enchantment, range)).build()


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
            SunCurse.translation = "Curse of Sol"
            SunCurse.descriptionKey?.translation = "Very strong light emanates from the item."
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
        withRegistry { out, r ->
            object: ItemLightSourceDataProvider(out, r.asCompletableFuture(), modid) {
                override fun generate(p0: Context) {
                    p0.add("sol_curse".id, LambItemPredicate(
                        /* items */ Optional.empty(),
                        /* count */ NumberRange.IntRange.ANY,
                        /* durability */ NumberRange.IntRange.ANY,
                        /* enchantments */ arrayOf(EnchantmentPredicate(SunCurse, NumberRange.IntRange.atLeast(1))),
                        /* storedEnchantments */ arrayOf(),
                        /* potion */ Optional.empty(),
                        /* nbt */ NbtPredicate.ANY
                    ), ItemLuminance.of(15), false)
                }
            }
        }
    }
}