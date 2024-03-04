package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.commands.util.ValidatedOfflinePlayerParser;
import me.wiefferink.areashop.features.FriendsFeature;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@Singleton
public class DelFriendCloudCommand extends CloudCommandBean {


    private static final CloudKey<OfflinePlayer> KEY_PLAYER = CloudKey.of("player", OfflinePlayer.class);
    private final MessageBridge messageBridge;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public DelFriendCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

    /**
     * Check if a person can remove friends.
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can remove friends, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (person.hasPermission("areashop.delfriendall")) {
            return true;
        }
        if (person instanceof Player player) {
            return region.isOwner(player) && player.hasPermission("areashop.delfriend");
        }
        return false;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("deletefriend", "delfriend");
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("deletefriend", "delfriend")
                .required(KEY_PLAYER, ValidatedOfflinePlayerParser.validatedOfflinePlayerParser(), this::suggestFriends)
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.delfriend") && !sender.hasPermission("areashop.delfriendall")) {
            throw new AreaShopCommandException("delfriend-noPermission");
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        OfflinePlayer friend = context.get(KEY_PLAYER);
        FriendsFeature friendsFeature = region.getFriendsFeature();
        if (sender.hasPermission("areashop.delfriendall")) {
            if ((region instanceof RentRegion rentRegion && !rentRegion.isRented())
                    || (region instanceof BuyRegion buyRegion && !buyRegion.isSold())) {
                throw new AreaShopCommandException("delfriend-noOwner", region);

            }
            if (!friendsFeature.getFriends().contains(friend.getUniqueId())) {
                throw new AreaShopCommandException("delfriend-notAdded", friend.getName(), region);

            }
            if (friendsFeature.deleteFriend(friend.getUniqueId(), sender)) {
                region.update();
                this.messageBridge.message(sender, "delfriend-successOther", friend.getName(), region);
            }
            return;
        }
        if (!sender.hasPermission("areashop.delfriend") || !(sender instanceof Player player)) {
            throw new AreaShopCommandException("delfriend-noPermission", region);
        }
        if (!region.isOwner(player)) {
            throw new AreaShopCommandException("delfriend-noPermissionOther", region);
        }
        if (!friendsFeature.getFriends().contains(friend.getUniqueId())) {
            throw new AreaShopCommandException("delfriend-notAdded", friend.getName(), region);
        } else if (friendsFeature.deleteFriend(friend.getUniqueId(), sender)) {
            region.update();
            this.messageBridge.message(sender, "delfriend-success", friend.getName(), region);
        }

    }

    private CompletableFuture<Iterable<Suggestion>> suggestFriends(
            @Nonnull CommandContext<CommandSender> context,
            @Nonnull CommandInput input
    ) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.delfriend")) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        GeneralRegion region;
        try {
            region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        } catch (AreaShopCommandException ignored) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        if (!sender.hasPermission("areashop.delfriendall")
                && sender instanceof Player player
                && !region.isOwner(player)
        ) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        String text = input.peekString();
        FriendsFeature friendsFeature = region.getFriendsFeature();
        Collection<Suggestion> suggestion = friendsFeature.getFriendNames().stream()
                .filter(name -> name.startsWith(text))
                .map(Suggestion::simple)
                .toList();
        return CompletableFuture.completedFuture(suggestion);
    }
}








