package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.commands.util.ValidatedOfflinePlayerParser;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.permission.Permission;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.UUID;

@Singleton
public class SetOwnerCommand extends AreashopCommandBean {

    private static final CloudKey<OfflinePlayer> KEY_PLAYER = CloudKey.of("player", OfflinePlayer.class);
    private final CommandFlag<GeneralRegion> regionFlag;

    private final MessageBridge messageBridge;

    @Inject
    public SetOwnerCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.setownerrent") || target.hasPermission("areashop.setownerbuy")) {
            return "help-setowner";
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
        return builder.literal("setowner")
                .permission(Permission.anyOf(Permission.of("areashop.setownerrent"), Permission.of("setownerbuy")))
                .required(KEY_PLAYER, ValidatedOfflinePlayerParser.validatedOfflinePlayerParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setowner");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.setownerrent") && !sender.hasPermission("areashop.setownerbuy")) {
            this.messageBridge.message(sender, "setowner-noPermission");
            return;
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        if (region instanceof RentRegion && !sender.hasPermission("areashop.setownerrent")) {
            this.messageBridge.message(sender, "setowner-noPermissionRent", region);
            return;
        }
        if (region instanceof BuyRegion && !sender.hasPermission("areashop.setownerbuy")) {
            this.messageBridge.message(sender, "setowner-noPermissionBuy", region);
            return;
        }
        OfflinePlayer player = context.get(KEY_PLAYER);
        if (!player.hasPlayedBefore()) {
            this.messageBridge.message(sender, "setowner-noPlayer", player.getName(), region);
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (region instanceof RentRegion rent) {
            if (rent.isRenter(uuid)) {
                // extend
                rent.setRentedUntil(rent.getRentedUntil() + rent.getDuration());
                rent.setRenter(uuid);
                this.messageBridge.message(sender, "setowner-succesRentExtend", region);
            } else {
                // change
                if (!rent.isRented()) {
                    rent.setRentedUntil(Calendar.getInstance().getTimeInMillis() + rent.getDuration());
                }
                rent.setRenter(uuid);
                this.messageBridge.message(sender, "setowner-succesRent", region);
            }
        } else if (region instanceof BuyRegion buy) {
            buy.setBuyer(uuid);
            this.messageBridge.message(sender, "setowner-succesBuy", region);
        }
        region.getFriendsFeature().deleteFriend(region.getOwner(), null);
        region.update();
        region.saveRequired();
    }

}








