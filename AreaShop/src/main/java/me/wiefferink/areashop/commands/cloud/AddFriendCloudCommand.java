package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.CloudCommandBean;
import me.wiefferink.areashop.commands.util.ExplicitGeneralRegionParser;
import me.wiefferink.areashop.commands.util.ImplicitGeneralRegionParser;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;

@Singleton
public class AddFriendCloudCommand extends CloudCommandBean {

    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final Plugin plugin;

    @Inject
    public AddFriendCloudCommand(
            @NonNull MessageBridge messageBridge,
            @NonNull IFileManager fileManager,
            @NonNull Plugin plugin
    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.plugin = plugin;
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
    protected Command.@NonNull Builder<? extends CommandSender> configureCommand(Command.@NonNull Builder<CommandSender> builder) {
        return builder.literal("addfriend")
                .senderType(Player.class)
                .required("friend", OfflinePlayerParser.offlinePlayerParser())
                .optional("region",
                        ParserDescriptor.of(new ExplicitGeneralRegionParser<>(this.fileManager), GeneralRegion.class),
                        ImplicitGeneralRegionParser.getInstance())
                .handler(this::handleCommand);
    }

    private void handleCommand(CommandContext<Player> context) {
        Player sender = context.sender();
        if (!sender.hasPermission("areashop.addfriend") && !sender.hasPermission("areashop.addfriendall")) {
            messageBridge.message(sender, "addfriend-noPermission");
            return;
        }
        GeneralRegion region = context.get("region");
        OfflinePlayer friend = context.get("friend");
        if (sender.hasPermission("areashop.addfriendall") && ((region instanceof RentRegion rentRegion && !rentRegion.isRented())
                || (region instanceof BuyRegion buyRegion && !buyRegion.isSold()))) {
            messageBridge.message(sender, "addfriend-noOwner", region);
            return;

        }
        if (!sender.hasPermission("areashop.addfriend")) {
            messageBridge.message(sender, "addfriend-noPermission", region);
            return;
        }
        if (!region.isOwner(sender)) {
            messageBridge.message(sender, "addfriend-noPermissionOther", region);
            return;
        }
        if (friend.hasPlayedBefore() && !friend.isOnline() && !plugin.getConfig()
                .getBoolean("addFriendNotExistingPlayers")) {
            messageBridge.message(sender, "addfriend-notVisited", friend.getName(), region);
        } else if (region.getFriendsFeature().getFriends().contains(friend.getUniqueId())) {
            messageBridge.message(sender, "addfriend-alreadyAdded", friend.getName(), region);
        } else if (region.isOwner(friend.getUniqueId())) {
            messageBridge.message(sender, "addfriend-self", friend.getName(), region);
        } else if (region.getFriendsFeature().addFriend(friend.getUniqueId(), sender)) {
            region.update();
            messageBridge.message(sender, "addfriend-success", friend.getName(), region);
        }
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("addfriend");
    }
}








