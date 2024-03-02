package me.wiefferink.areashop.commands.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AcceptedValuesParser<C> implements ArgumentParser<C, String>, SuggestionProvider<C> {

    private final Supplier<@NonNull Collection<String>> valuesProvider;
    private final boolean caseSensitive;

    private final String failureMessageKey;

    private AcceptedValuesParser(
            @NonNull Supplier<@NonNull Collection<String>> valuesProvider,
            @NonNull String failureMessageKey,
            boolean caseSensitive

    ) {
        this.valuesProvider = valuesProvider;
        this.failureMessageKey = failureMessageKey;
        this.caseSensitive = caseSensitive;
    }

    private static Collection<Suggestion> provideSuggestions(@NonNull List<String> values,
                                                             @NonNull CommandInput input) {
        String text = input.peekString();
        return values.stream()
                .filter(s -> s.startsWith(text))
                .map(Suggestion::simple)
                .toList();
    }

    public static <C> AcceptedValuesParser<C> ofConstant(
            @NonNull Collection<String> acceptedValues,
            @NonNull String failureMessageKey,
            boolean caseSensitive
    ) {
        Collection<String> copy = List.copyOf(acceptedValues);
        return new AcceptedValuesParser<>(() -> copy, failureMessageKey, caseSensitive);
    }

    public static <C> AcceptedValuesParser<C> of(
            @NonNull Supplier<@NonNull Collection<String>> supplier,
            @NonNull String failureMessageKey,
            boolean caseSensitive
    ) {
        return new AcceptedValuesParser<>(supplier, failureMessageKey, caseSensitive);
    }

    public static <C> AcceptedValuesParser<C> ofCached(
            @NonNull Supplier<@NonNull Collection<String>> supplier,
            @NonNull String failureMessageKey,
            boolean caseSensitive
    ) {
        return ofConstant(supplier.get(), failureMessageKey, caseSensitive);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(@NonNull CommandContext<@NonNull C> commandContext,
                                                               @NonNull CommandInput commandInput) {
        String toTest;
        if (this.caseSensitive) {
            toTest = commandInput.peekString().toLowerCase(Locale.ENGLISH);
        } else {
            toTest = commandInput.peekString();
        }
        Collection<String> acceptedValues = this.valuesProvider.get();
        if (acceptedValues.contains(toTest)) {
            return ArgumentParseResult.success(toTest);
        }
        return ArgumentParseResult.failure(new AreaShopCommandException(this.failureMessageKey));
    }

    @Override
    public @NonNull CompletableFuture<@NonNull Iterable<@NonNull Suggestion>> suggestionsFuture(
            @NonNull CommandContext<C> context,
            @NonNull CommandInput input
    ) {
        String text = input.peekString();
        Iterable<Suggestion> suggestions = this.valuesProvider.get().stream()
                .filter(s -> s.startsWith(text))
                .map(Suggestion::simple)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}
