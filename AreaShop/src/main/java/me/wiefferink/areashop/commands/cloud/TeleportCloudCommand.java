package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class TeleportCloudCommand extends CloudCommandBean {

    private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);
    private static final CommandFlag<Void> KEY_TO_SIGN = CommandFlag.builder("to-sign").build();
    private final MessageBridge messageBridge;

    @Inject
    public TeleportCloudCommand(@Nonnull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    /**
     * Check if a person can teleport to the region (assuming he is not teleporting to a sign).
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can teleport to it, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (!(person instanceof Player player)) {
            return false;
        }
        return player.hasPermission("areashop.teleportall")
                || region.isOwner(player) && player.hasPermission("areashop.teleport")
                || region.isAvailable() && player.hasPermission("areashop.teleportavailable")
                || region.getFriendsFeature().getFriends().contains(player.getUniqueId()) && player.hasPermission(
                "areashop.teleportfriend");
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.teleportall") || target.hasPermission("areashop.teleport")) {
            return "help-teleport";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @NotNull
    @Override
    protected Command.Builder<? extends CommandSender> configureCommand(@NotNull Command.Builder<CommandSender> builder) {
        return builder.literal("teleport", "tp")
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("teleport", "tp");
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        Player player = context.sender();
        if (!player.hasPermission("areashop.teleport") && !player.hasPermission("areashop.teleportall") && !player.hasPermission(
                "areashop.teleportavailable") && !player.hasPermission("areashop.teleportavailablesign") && !player.hasPermission(
                "areashop.teleportsign") && !player.hasPermission("areashop.teleportsignall") && !player.hasPermission(
                "areashop.teleportfriend") && !player.hasPermission("teleportfriendsign")) {
            this.messageBridge.message(player, "teleport-noPermission");
            return;
        }
        GeneralRegion region = context.get(KEY_REGION);
        boolean toSign = context.flags().contains(KEY_TO_SIGN);
        region.getTeleportFeature().teleportPlayer(player, toSign);
    }
}
