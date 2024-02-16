package me.wiefferink.areashop.commands.util;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Collections;

public class WorldGuardRegionParser<C> implements ArgumentParser<C, ProtectedRegion> {
    private final WorldGuardInterface worldGuardInterface;
    private final String worldKey;

    public WorldGuardRegionParser(
            @NonNull String worldKey,
            @NonNull WorldGuardInterface worldGuardInterface
    ) {
        this.worldKey = worldKey;
        this.worldGuardInterface = worldGuardInterface;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull ProtectedRegion> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                                        @NonNull CommandInput commandInput) {
        World world = commandContext.get(this.worldKey);
        String regionName = commandInput.readString();
        RegionManager regionManager = this.worldGuardInterface.getRegionManager(world);
        if (regionManager == null) {
            return ArgumentParseResult.failure(new IllegalArgumentException("No region manager for world: " + world.getName()));
        }
        ProtectedRegion protectedRegion = regionManager.getRegion(regionName);
        if (protectedRegion != null) {
            return ArgumentParseResult.success(protectedRegion);
        }
        ParserException exception = new GenericArgumentParseException(
                WorldGuardRegionParser.class,
                commandContext,
                Caption.of("cmd-noRegion"),
                CaptionVariable.of("region", regionName)
        );
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((commandContext, input) -> {
            C sender = commandContext.sender();
            if (!(sender instanceof Player player) || (!player.hasPermission("areashop.createrent")
                    && !player.hasPermission("areashop.createbuy"))
            ) {
                return Collections.emptyList();
            }
            return this.worldGuardInterface.getRegionManager(player.getWorld()).getRegions()
                    .values()
                    .stream()
                    .map(ProtectedRegion::getId)
                    .toList();
        });
    }
}
