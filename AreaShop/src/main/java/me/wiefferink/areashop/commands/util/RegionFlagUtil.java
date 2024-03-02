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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;

import java.util.List;

public final class RegionFlagUtil {

    private RegionFlagUtil() {
        throw new IllegalStateException("Cannot instantiate static utility class");
    }

    @NonNull
    public static CommandFlag<GeneralRegion> createDefault(@NonNull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new GeneralRegionParser<>(fileManager), GeneralRegion.class))
                .build();
    }

    @NonNull
    public static <C extends Entity & CommandSender> GeneralRegion createOrParseRegion(
            @NonNull CommandContext<C> context,
            @NonNull CommandFlag<GeneralRegion> flag
    ) throws AreaShopCommandException {
        GeneralRegion region = context.flags().get(flag);
        if (region != null) {
            return region;
        }
        Location location = context.sender().getLocation();
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

    public static CommandFlag<BuyRegion> createDefaultBuy(@NonNull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new BuyRegionParser<>(fileManager), BuyRegion.class))
                .build();
    }
    
    public static CommandFlag<RentRegion> createDefaultRent(@NonNull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new RentRegionParser<>(fileManager), RentRegion.class))
                .build();
    }
    
    public static BuyRegion getOrParseBuyRegion(@NonNull CommandContext<Player> context, CommandFlag<BuyRegion> flag) {
        BuyRegion buyRegion = context.flags().get(flag);
        if (buyRegion != null) {
            return buyRegion;
        }
        List<BuyRegion> regions = Utils.getImportantBuyRegions(context.sender().getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.get(0);
    }

    public static RentRegion getOrParseRentRegion(@NonNull CommandContext<Player> context, CommandFlag<RentRegion> flag) {
        RentRegion rentRegion = context.flags().get(flag);
        if (rentRegion != null) {
            return rentRegion;
        }
        List<RentRegion> regions = Utils.getImportantRentRegions(context.sender().getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.get(0);
    }
    
}
