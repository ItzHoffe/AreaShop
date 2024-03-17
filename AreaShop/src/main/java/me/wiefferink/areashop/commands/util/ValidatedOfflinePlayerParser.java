package me.wiefferink.areashop.commands.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.bukkit.BukkitCommandContextKeys;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public class ValidatedOfflinePlayerParser<C> implements ArgumentParser<C, OfflinePlayer>, BlockingSuggestionProvider.Strings<C> {

    public static <C> ParserDescriptor<C, OfflinePlayer> validatedOfflinePlayerParser() {
        return ParserDescriptor.of(new ValidatedOfflinePlayerParser<>(), OfflinePlayer.class);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NonNull ArgumentParseResult<OfflinePlayer> parse(
            final @NonNull CommandContext<C> commandContext,
            final @NonNull CommandInput commandInput
    ) {
        final String input = commandInput.readString();
        if (input.length() > 16) {
            return ArgumentParseResult.failure(new AreaShopCommandException("cmd-invalidPlayer", input));
        }
        final Player onlinePlayer = Bukkit.getPlayerExact(input);
        if (onlinePlayer != null) {
            return ArgumentParseResult.success(onlinePlayer);
        }
        final OfflinePlayer player;
        try {
            player = Bukkit.getOfflinePlayer(input);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(new AreaShopCommandException("cmd-invalidPlayer", input));
        }
        if (!player.hasPlayedBefore()) {
            return ArgumentParseResult.failure(new AreaShopCommandException("cmd-invalidPlayer", input));
        }
        return ArgumentParseResult.success(player);
    }

    @Override
    public @NonNull Iterable<@NonNull String> stringSuggestions(
            final @NonNull CommandContext<C> commandContext,
            final @NonNull CommandInput input
    ) {
        final CommandSender sender = commandContext.get(BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER);
        return Bukkit.getOnlinePlayers().stream()
                .filter(onlinePlayer -> !(sender instanceof Player player && !((Player) sender).canSee(player)))
                .map(Player::getName)
                .toList();
    }
}
