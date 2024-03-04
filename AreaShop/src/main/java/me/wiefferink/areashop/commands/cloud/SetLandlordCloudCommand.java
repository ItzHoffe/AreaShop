package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.commands.util.ValidatedOfflinePlayerParser;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SetLandlordCloudCommand extends CloudCommandBean {

    private static final CloudKey<OfflinePlayer> KEY_PLAYER = CloudKey.of("player", OfflinePlayer.class);

    private final MessageBridge messageBridge;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public SetLandlordCloudCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionFlagUtil.createDefault(fileManager);
    }


    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.setlandlord")) {
            return "help-setlandlord";
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
        return builder.literal("setlandlord")
                .permission("areashop.setlandlord")
                .required(KEY_PLAYER, ValidatedOfflinePlayerParser.validatedOfflinePlayerParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setlandlord");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.setlandlord")) {
            this.messageBridge.message(sender, "setlandlord-noPermission");
            return;
        }
        GeneralRegion region = RegionFlagUtil.getOrParseRegion(context, this.regionFlag);
        OfflinePlayer player = context.get(KEY_PLAYER);
        String playerName = player.getName();
        region.setLandlord(player.getUniqueId(), playerName);
        region.update();
        this.messageBridge.message(sender, "setlandlord-success", playerName, region);
    }

}
