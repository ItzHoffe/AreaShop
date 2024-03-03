package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;

@Singleton
public class RentCloudCommand extends CloudCommandBean {

    private final CommandFlag<RentRegion> rentRegionFlag;

    @Inject
    public RentCloudCommand(@Nonnull IFileManager fileManager) {
        this.rentRegionFlag = RegionFlagUtil.createDefaultRent(fileManager);
    }

    @Override
    public String stringDescription() {
        return "Allows you to rent a region";
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("rent");
    }


    @Override
    protected @Nonnull Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder
                .literal("rent")
                .permission("areashop.rent")
                .flag(this.rentRegionFlag)
                .senderType(Player.class)
                .handler(this::handleCommand);
    }

    private void handleCommand(@Nonnull CommandContext<Player> context) {
        RentRegion region = RegionFlagUtil.getOrParseRentRegion(context, this.rentRegionFlag);
        region.rent(context.sender());
    }

}
