package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;

public class RentRegionParser<C> implements ArgumentParser<C, RentRegion> {

    private final IFileManager fileManager;

    public RentRegionParser(@Nonnull IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public @Nonnull ArgumentParseResult<RentRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                                   @Nonnull CommandInput commandInput) {
        String input = commandInput.peekString();
        RentRegion region = this.fileManager.getRent(input);
        if (region != null) {
            commandInput.readString();
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("buy-notBuyable", input);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> {
                    String text = input.peekString();
                    return this.fileManager.getRentNames()
                            .stream()
                            .filter(name -> name.startsWith(text))
                            .toList();
                }
        );
    }
}
