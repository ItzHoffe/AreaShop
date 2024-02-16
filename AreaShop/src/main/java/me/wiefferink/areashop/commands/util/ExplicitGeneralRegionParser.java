package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.units.qual.A;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;

public class ExplicitGeneralRegionParser<C> implements ArgumentParser<C, GeneralRegion>  {

    private final IFileManager fileManager;

    public ExplicitGeneralRegionParser(@NonNull IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull GeneralRegion> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                                      @NonNull CommandInput commandInput) {
        String input = commandInput.readInput();
        GeneralRegion region = this.fileManager.getRegion(input);
        if (region != null) {
            return ArgumentParseResult.success(region);
        }
        GenericArgumentParseException exception = new GenericArgumentParseException(
                ExplicitGeneralRegionParser.class,
                commandContext,
                Caption.of("cmd-notRegistered")
        );
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> this.fileManager.getRegionNames());
    }
}
