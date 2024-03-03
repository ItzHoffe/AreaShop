package me.wiefferink.areashop.commands.util;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;
import java.util.Collections;

public class WorldGuardRegionParser<C extends Entity> implements ArgumentParser<C, ProtectedRegion> {
    private final WorldGuardInterface worldGuardInterface;
    private final CommandFlag<World> worldFlag;

    public WorldGuardRegionParser(
            @Nonnull CommandFlag<World> worldFlag,
            @Nonnull WorldGuardInterface worldGuardInterface
    ) {
        this.worldFlag = worldFlag;
        this.worldGuardInterface = worldGuardInterface;
    }

    @Override
    public @Nonnull ArgumentParseResult<ProtectedRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                                        @Nonnull CommandInput commandInput) {
        World world = WorldFlagUtil.parseOrDetectWorld(commandContext, worldFlag);
        String regionName = commandInput.peekString();
        RegionManager regionManager = this.worldGuardInterface.getRegionManager(world);
        if (regionManager == null) {
            return ArgumentParseResult.failure(new IllegalArgumentException("No region manager for world: " + world.getName()));
        }
        ProtectedRegion protectedRegion = regionManager.getRegion(regionName);
        if (protectedRegion != null) {
            commandInput.readString();
            return ArgumentParseResult.success(protectedRegion);
        }
        AreaShopCommandException exception = new AreaShopCommandException("cmd-noRegion", regionName);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((commandContext, input) -> {
            C sender = commandContext.sender();
            if (!(sender instanceof Player player)) {
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