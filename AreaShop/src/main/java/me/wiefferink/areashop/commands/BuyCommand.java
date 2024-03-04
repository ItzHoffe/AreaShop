package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class BuyCommand extends AreashopCommandBean {

    private final CommandFlag<BuyRegion> buyRegionFlag;

    @Inject
    public BuyCommand(@Nonnull IFileManager fileManager) {
        this.buyRegionFlag = RegionFlagUtil.createDefaultBuy(fileManager);
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if(target.hasPermission("areashop.buy")) {
            return "help-buy";
        }
        return null;
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
                .flag(this.buyRegionFlag)
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        if (!context.hasPermission("areashop.buy")) {
            throw new AreaShopCommandException("buy-noPermission");
        }
        BuyRegion region = RegionFlagUtil.getOrParseBuyRegion(context, this.buyRegionFlag);
        region.buy(context.sender());
    }

}
