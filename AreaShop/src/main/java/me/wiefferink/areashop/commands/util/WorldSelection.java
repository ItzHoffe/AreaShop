package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.yaml.snakeyaml.parser.ParserException;

public record WorldSelection(@NonNull World world, @NonNull WorldEditSelection selection) {

    public static WorldSelection fromPlayer(@NonNull Player player,
                                            @NonNull WorldEditInterface worldEditInterface) throws ParserException {
        WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
        if (selection == null) {
            throw new AreaShopCommandException("cmd-noSelection");
        }
        World world = selection.getWorld();
        return new WorldSelection(world, selection);
    }

}