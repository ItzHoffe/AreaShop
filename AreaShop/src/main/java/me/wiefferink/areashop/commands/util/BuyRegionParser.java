package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

public class BuyRegionParser<C> implements ArgumentParser<C, BuyRegion> {

    private final IFileManager fileManager;

    public BuyRegionParser(@NonNull IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull BuyRegion> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                                  @NonNull CommandInput commandInput) {
        String input = commandInput.readInput();
        BuyRegion region = this.fileManager.getBuy(input);
        if (region != null) {
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("buy-notBuyable");
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> {
                    String text = input.peekString();
                    return this.fileManager.getBuyNames()
                            .stream()
                            .filter(name -> name.startsWith(text))
                            .toList();
                }
        );
    }
}
