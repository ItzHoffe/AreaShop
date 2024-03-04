package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;

public class GeneralRegionParser<C> implements ArgumentParser<C, GeneralRegion>  {

    protected final IFileManager fileManager;

    public GeneralRegionParser(@Nonnull IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    public static <C> ParserDescriptor<C, GeneralRegion> generalRegionParser(@Nonnull IFileManager fileManager) {
        return ParserDescriptor.of(new GeneralRegionParser<>(fileManager), GeneralRegion.class);
    }

    @Override
    public @Nonnull ArgumentParseResult<GeneralRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                                      @Nonnull CommandInput commandInput) {
        String input = commandInput.peekString();
        GeneralRegion region = this.fileManager.getRegion(input);
        if (region != null) {
            commandInput.readString();
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("cmd-notRegistered", input);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> this.fileManager.getRegionNames());
    }
}
