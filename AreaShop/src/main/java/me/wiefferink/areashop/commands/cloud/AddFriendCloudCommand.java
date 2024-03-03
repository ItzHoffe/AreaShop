package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class AddFriendCloudCommand extends CloudCommandBean {

    private static final CloudKey<OfflinePlayer> KEY_FRIEND = CloudKey.of("friend", OfflinePlayer.class);
    private final CommandFlag<GeneralRegion> regionFlag;
    private final MessageBridge messageBridge;
    private final Plugin plugin;

    @Inject
    public AddFriendCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull Plugin plugin
    ) {
        this.messageBridge = messageBridge;
        this.plugin = plugin;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.addfriendall") || target.hasPermission("areashop.addfriend")) {
            return "help-addFriend";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return "Allows you to add friends to your regions";
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("addfriend")
                .senderType(Player.class)
                .required(KEY_FRIEND, OfflinePlayerParser.offlinePlayerParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    private void handleCommand(CommandContext<Player> context) {
        Player sender = context.sender();
        if (!sender.hasPermission("areashop.addfriend") && !sender.hasPermission("areashop.addfriendall")) {
           this.messageBridge.message(sender, "addfriend-noPermission");
            return;
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        OfflinePlayer friend = context.get(KEY_FRIEND);
        if (sender.hasPermission("areashop.addfriendall") && ((region instanceof RentRegion rentRegion && !rentRegion.isRented())
                || (region instanceof BuyRegion buyRegion && !buyRegion.isSold()))) {
           this.messageBridge.message(sender, "addfriend-noOwner", region);
            return;

        }
        if (!sender.hasPermission("areashop.addfriend")) {
           this.messageBridge.message(sender, "addfriend-noPermission", region);
            return;
        }
        if (!region.isOwner(sender)) {
           this.messageBridge.message(sender, "addfriend-noPermissionOther", region);
            return;
        }
        if (friend.hasPlayedBefore() && !friend.isOnline() && !plugin.getConfig()
                .getBoolean("addFriendNotExistingPlayers")) {
           this.messageBridge.message(sender, "addfriend-notVisited", friend.getName(), region);
        } else if (region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
           this.messageBridge.message(sender, "addfriend-alreadyAdded", friend.getName(), region);
        } else if (region.isOwner(friend.getUniqueId())) {
           this.messageBridge.message(sender, "addfriend-self", friend.getName(), region);
        } else if (region.getFriendsFeature().addFriend(friend.getUniqueId(), sender)) {
            region.update();
           this.messageBridge.message(sender, "addfriend-success", friend.getName(), region);
        }
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("addfriend");
    }
}








