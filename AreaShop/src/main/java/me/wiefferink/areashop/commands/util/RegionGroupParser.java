package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RegionGroup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegionGroupParser<C> implements ArgumentParser<C, RegionGroup>, SuggestionProvider<C> {

    private final IFileManager fileManager;
    private final String failureMessageKey;

    public RegionGroupParser(@NonNull IFileManager fileManager, @NonNull String failureMessageKey) {
        this.fileManager = fileManager;
        this.failureMessageKey = failureMessageKey;
    }


    @Override
    public @NonNull ArgumentParseResult<@NonNull RegionGroup> parse(
            @NonNull CommandContext<@NonNull C> commandContext,
            @NonNull CommandInput commandInput
    ) {
        String input = commandInput.peekString();
        RegionGroup regionGroup = this.fileManager.getGroup(input);
        if (regionGroup != null) {
            commandInput.readString();
            return ArgumentParseResult.success(regionGroup);
        }
        return ArgumentParseResult.failure(new AreaShopCommandException(this.failureMessageKey, input));
    }

    @Override
    public @NonNull CompletableFuture<@NonNull Iterable<@NonNull Suggestion>> suggestionsFuture(
            @NonNull CommandContext<C> context,
            @NonNull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getGroupNames().stream()
                .filter(name -> name.startsWith(text))
                .map(Suggestion::simple)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}

