package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public final class RegionFlagUtil {

    private RegionFlagUtil() {
        throw new IllegalStateException("Cannot instantiate static utility class");
    }

    @Nonnull
    public static CommandFlag<GeneralRegion> createDefault(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new GeneralRegionParser<>(fileManager), GeneralRegion.class))
                .build();
    }

    @Nonnull
    public static Collection<GeneralRegion> getOrParseRegionsInSel(
            @Nonnull CommandContext<CommandSender> context,
            @Nonnull CommandFlag<GeneralRegion> regionFlag
    ) {
        CommandSender sender = context.sender();
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-weOnlyByPlayer");
        }
        GeneralRegion declaredRegion = context.flags().get(regionFlag);
        if (declaredRegion != null) {
            return List.of(declaredRegion);
        }
        Location location = player.getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        if (!regions.isEmpty()) {
            return regions;

        }
        throw new AreaShopCommandException("cmd-noRegionsAtLocation");
    }

    @Nonnull
    public static <C> GeneralRegion getOrParseRegion(
            @Nonnull CommandContext<C> context,
            @Nonnull CommandFlag<GeneralRegion> flag
    ) throws AreaShopCommandException {
        GeneralRegion region = context.flags().get(flag);
        if (region != null) {
            return region;
        }
        C sender = context.sender();
        if (!(sender instanceof Entity entity)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        Location location = entity.getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        String errorMessageKey;
        if (regions.isEmpty()) {
            errorMessageKey = "cmd-noRegionsAtLocation";
        } else if (regions.size() > 1) {
            errorMessageKey = "cmd-moreRegionsAtLocation";
        } else {
            return regions.get(0);
        }
        throw new AreaShopCommandException(errorMessageKey);
    }


    @Nonnull
    public static CommandFlag<BuyRegion> createDefaultBuy(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new BuyRegionParser<>(fileManager), BuyRegion.class))
                .build();
    }

    @Nonnull
    public static CommandFlag<RentRegion> createDefaultRent(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new RentRegionParser<>(fileManager), RentRegion.class))
                .build();
    }

    @Nonnull
    public static BuyRegion getOrParseBuyRegion(@Nonnull CommandContext<CommandSender> context, CommandFlag<BuyRegion> flag) {
        BuyRegion buyRegion = context.flags().get(flag);
        if (buyRegion != null) {
            return buyRegion;
        }
        CommandSender sender = context.sender();
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        List<BuyRegion> regions = Utils.getImportantBuyRegions(player.getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.get(0);
    }

    @Nonnull
    public static RentRegion getOrParseRentRegion(@Nonnull CommandContext<CommandSender> context, CommandFlag<RentRegion> flag) {
        RentRegion rentRegion = context.flags().get(flag);
        if (rentRegion != null) {
            return rentRegion;
        }
        CommandSender sender = context.sender();
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        List<RentRegion> regions = Utils.getImportantRentRegions(player.getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.get(0);
    }
    
}
