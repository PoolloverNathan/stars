package org.eu.net.pool.fabric.cots.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import org.eu.net.pool.fabric.cots.SilenceCurse
import poollovernathan.fabric.DataContext

fun init() {

}

fun datagen(gen: FabricDataGenerator) {
    with(DataContext(gen)) {
        lang("en_us") {
            SilenceCurse.translation = "Curse of Silence"
        }
    }
}