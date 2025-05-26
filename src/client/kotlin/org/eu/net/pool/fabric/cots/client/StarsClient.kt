package org.eu.net.pool.fabric.cots.client

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider
import net.minecraft.data.client.BlockStateModelGenerator
import net.minecraft.data.client.ItemModelGenerator
import org.eu.net.pool.fabric.cots.SilenceCurse
import org.eu.net.pool.fabric.cots.StoneArmorMaterial
import org.eu.net.pool.fabric.cots.StoneCurse
import poollovernathan.fabric.DataContext

fun init() {

}

fun datagen(gen: FabricDataGenerator) {
    with(DataContext(gen)) {
        lang("en_us") {
            SilenceCurse.translation = "Curse of Silence"
            StoneCurse.translation = "Curse of Terra" // t3rracat reference??
            StoneArmorMaterial.armorItems.forEach { (type, item) ->
                item.translation = "Stone ${type.name[0] + type.name.substring(1).lowercase()}"
            }
        }
        provider {
            object: FabricModelProvider(it) {
                override fun generateBlockStateModels(p0: BlockStateModelGenerator) {

                }
                override fun generateItemModels(p0: ItemModelGenerator) {
                    StoneArmorMaterial.armorItems.values.forEach(p0::registerArmor)
                }
            }
        }
    }
}