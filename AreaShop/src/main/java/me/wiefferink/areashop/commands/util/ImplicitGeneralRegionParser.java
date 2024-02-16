package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;

import java.util.List;

public class ImplicitGeneralRegionParser<C extends Entity> implements DefaultValue<C, GeneralRegion> {

    @SuppressWarnings("rawtypes")
    private static final ImplicitGeneralRegionParser INSTANCE = new ImplicitGeneralRegionParser();

    private ImplicitGeneralRegionParser() {
    }

    @SuppressWarnings("unchecked")
    public static <C extends Entity> ImplicitGeneralRegionParser<C> getInstance() {
        return INSTANCE;
    }

    @Override
    public @NonNull ArgumentParseResult<GeneralRegion> evaluateDefault(
            @NonNull CommandContext<@NonNull C> commandContext
    ) {
        // get the region by location
        C sender = commandContext.sender();
        List<GeneralRegion> regions = Utils.getImportantRegions(sender.getLocation());
        Caption error;
        if (regions.isEmpty()) {
            error = Caption.of("cmd-noRegionsAtLocation");
        } else if (regions.size() > 1) {
            error = Caption.of("cmd-moreRegionsAtLocation");
        } else {
            return ArgumentParseResult.success(regions.get(0));
        }
        return ArgumentParseResult.failure(new GenericArgumentParseException(
                        ImplicitGeneralRegionParser.class,
                        commandContext,
                        error
                )
        );
    }
}
