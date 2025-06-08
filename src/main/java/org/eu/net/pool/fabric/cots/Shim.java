package org.eu.net.pool.fabric.cots;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import poollovernathan.fabric.CommandContext;

class Shim {
    static <T> BlockState setPropertyUnchecked(BlockState prop, Property<?> p, T v) {
        return prop.withIfExists((Property) p, (Comparable) v);
    }

    static CommandContext<ServerCommandSource> makeContext(CommandDispatcher<ServerCommandSource> d) {
        return new CommandContext.NodeContext<>(d.getRoot());
    }
}
