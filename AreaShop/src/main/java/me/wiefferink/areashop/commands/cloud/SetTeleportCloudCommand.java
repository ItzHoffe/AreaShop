package me.wiefferink.areashop.commands.cloud;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SetTeleportCloudCommand extends CloudCommandBean {

    private static final CommandFlag<Void> FLAG_RESET = CommandFlag.builder("reset").build();
    private final CommandFlag<GeneralRegion> regionFlag;

    private final MessageBridge messageBridge;

    @Inject
    public SetTeleportCloudCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }

    /**
     * Check if a person can set the teleport location of the region.
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can set the teleport location, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (!(person instanceof Player player)) {
            return false;
        }
        return player.hasPermission("areashop.setteleportall")
                || region.isOwner(player) && player.hasPermission("areashop.setteleport");
    }

    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.setteleportall") || target.hasPermission("areashop.setteleport")) {
            return "help-setteleport";
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
        return builder.literal("settp")
                .senderType(Player.class)
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("settp");
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        Player player = context.sender();
        if (!player.hasPermission("areashop.setteleport") && !player.hasPermission("areashop.setteleportall")) {
            this.messageBridge.message(player, "setteleport-noPermission");
            return;
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);

        boolean owner;
        if (region instanceof RentRegion rentRegion) {
            owner = player.getUniqueId().equals(rentRegion.getRenter());
        } else if (region instanceof BuyRegion buyRegion) {
            owner = player.getUniqueId().equals(buyRegion.getBuyer());
        } else {
            // FIXME log error
            return;
        }
        if (!player.hasPermission("areashop.setteleport")) {
            throw new AreaShopCommandException("setteleport-noPermission", region);
        } else if (!owner && !player.hasPermission("areashop.setteleportall")) {
            throw new AreaShopCommandException("setteleport-noPermissionOther", region);
        }
        boolean reset = context.flags().contains(FLAG_RESET);
        if (reset) {
            region.getTeleportFeature().setTeleport(null);
            region.update();
            this.messageBridge.message(player, "setteleport-reset", region);
        }
        ProtectedRegion wgRegion = region.getRegion();
        Location location = player.getLocation();
        if (!player.hasPermission("areashop.setteleportoutsideregion") && (wgRegion == null || !wgRegion.contains(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()))
        ) {
            this.messageBridge.message(player, "setteleport-notInside", region);
            return;
        }
        region.getTeleportFeature().setTeleport(location);
        region.update();
        this.messageBridge.message(player, "setteleport-success", region);
    }

}
