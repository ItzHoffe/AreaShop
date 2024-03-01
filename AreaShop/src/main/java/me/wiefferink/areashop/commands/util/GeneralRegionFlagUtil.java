package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;

import java.util.List;

public final class GeneralRegionFlagUtil {

    private GeneralRegionFlagUtil() {
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
    ) throws GenericArgumentParseException {
        GeneralRegion region = context.flags().get(flag);
        if (region != null) {
            return region;
        }
        Location location = context.sender().getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        Caption error;
        if (regions.isEmpty()) {
            error = Caption.of("cmd-noRegionsAtLocation");
        } else if (regions.size() > 1) {
            error = Caption.of("cmd-moreRegionsAtLocation");
        } else {
            return regions.get(0);
        }
        throw new GenericArgumentParseException(WorldFlagUtil.class, context, error);
    }
}
