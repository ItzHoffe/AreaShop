package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class BuyCloudCommand extends CloudCommandBean {

    private final CommandFlag<BuyRegion> buyRegionFlag;

    @Inject
    public BuyCloudCommand(@Nonnull IFileManager fileManager) {
        this.buyRegionFlag = RegionFlagUtil.createDefaultBuy(fileManager);
    }

    @Override
    public String stringDescription() {
        return "Allows you to buy a region";
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("buy");
    }


    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder
                .literal("buy")
                .permission("areashop.buy")
                .flag(this.buyRegionFlag)
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        BuyRegion region = RegionFlagUtil.getOrParseBuyRegion(context, this.buyRegionFlag);
        region.buy(context.sender());
    }

}
