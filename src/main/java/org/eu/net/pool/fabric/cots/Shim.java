package org.eu.net.pool.fabric.cots;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

class Shim {
    static <T> BlockState setPropertyUnchecked(BlockState prop, Property<?> p, T v) {
        return prop.withIfExists((Property) p, (Comparable) v);
    }
}
