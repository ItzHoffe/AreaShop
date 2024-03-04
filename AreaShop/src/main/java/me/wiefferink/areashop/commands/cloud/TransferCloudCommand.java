package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TransferCloudCommand extends CloudCommandBean {

    private static final CloudKey<OfflinePlayer> KEY_PLAYER = CloudKey.of("player", OfflinePlayer.class);
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public TransferCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        ParserDescriptor<Player, GeneralRegion> regionParser =
                ParserDescriptor.of(new GeneralRegionParser<>(fileManager, this::suggestRegions), GeneralRegion.class);
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFlag = CommandFlag.builder("region")
                .withComponent(regionParser)
                .build();
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (!target.hasPermission("areashop.transfer")) {
            return null;
        }
        return "help-transfer";
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @NotNull
    @Override
    protected Command.Builder<? extends CommandSender> configureCommand(@NotNull Command.Builder<CommandSender> builder) {
        return builder.literal("transfer")
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("transfer");
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        Player sender = context.sender();
        if (!sender.hasPermission("areashop.transfer")) {
            throw new AreaShopCommandException("transfer-noPermission");
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        if (!region.isTransferEnabled()) {
            throw new AreaShopCommandException("transfer-disabled");
        }
        OfflinePlayer targetPlayer = context.get(KEY_PLAYER);
        String targetPlayerName = targetPlayer.getName();
        if (Objects.equals(sender, targetPlayer)) {
            throw new AreaShopCommandException("transfer-transferSelf");
        }
        if (!targetPlayer.hasPlayedBefore()) {
            // Unknown player
            throw new AreaShopCommandException("transfer-noPlayer", targetPlayerName);
        }
        if (region.isLandlord(sender.getUniqueId())) {
            // Transfer ownership if same as landlord
            region.getFriendsFeature().deleteFriend(region.getOwner(), null);
            region.setOwner(targetPlayer.getUniqueId());
            region.setLandlord(targetPlayer.getUniqueId(), targetPlayerName);
            this.messageBridge.message(sender, "transfer-transferred-owner", targetPlayerName, region);
            this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-owner", targetPlayerName, region);
            region.update();
            region.saveRequired();
            return;
        }
        if (!region.isOwner(sender.getUniqueId())) {
            // Cannot transfer tenant if we aren't the current tenant
            throw new AreaShopCommandException("transfer-notCurrentTenant");
        }
        region.getFriendsFeature().deleteFriend(region.getOwner(), null);
        // Swap the owner/occupant (renter or buyer)
        region.setOwner(targetPlayer.getUniqueId());

        this.messageBridge.message(sender, "transfer-transferred-tenant", targetPlayerName, region);
        this.messageBridge.messagePersistent(targetPlayer, "transfer-transferred-tenant", targetPlayerName, region);
        region.update();
        region.saveRequired();
    }

    private CompletableFuture<Iterable<Suggestion>> suggestRegions(
            @Nonnull CommandContext<Player> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        UUID uuid = context.sender().getUniqueId();
        List<Suggestion> suggestions = this.fileManager.getRegions()
                .stream()
                .filter(region -> region.isOwner(uuid) || region.isLandlord(uuid))
                .map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::simple)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }

}
