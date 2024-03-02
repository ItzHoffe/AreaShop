package me.wiefferink.areashop.commands.util;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

public final class WorldFlagUtil {

    public static final CommandFlag<World> DEFAULT_WORLD_FLAG = CommandFlag.builder("world")
            .withComponent(WorldParser.worldParser())
            .build();

    private WorldFlagUtil() {
        throw new IllegalStateException("Cannot instantiate static util class");
    }

    @NonNull
    public static <C extends Entity> World parseOrDetectWorld(@NonNull CommandContext<C> context) {
        return parseOrDetectWorld(context, DEFAULT_WORLD_FLAG);
    }

    @NonNull
    public static <C extends Entity> World parseOrDetectWorld(
            @NonNull CommandContext<C> context,
            @NonNull CommandFlag<World> flag
    ) {
        World world = context.flags().get(flag);
        if (world != null) {
            return world;
        }
        return context.sender().getWorld();
    }

}
