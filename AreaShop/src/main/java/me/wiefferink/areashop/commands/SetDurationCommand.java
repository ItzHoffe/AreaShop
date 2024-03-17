package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.DurationInputParser;
import me.wiefferink.areashop.commands.util.RegionFlagUtil;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.DurationInput;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.DurationParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.sql.Time;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Singleton
public class SetDurationCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_DURATION = CloudKey.of("amount", String.class);
    private static final DurationInputParser<CommandSender> DURATION_PARSER = new DurationInputParser<>();
    private final MessageBridge messageBridge;
    private final CommandFlag<RentRegion> regionFlag;

    @Inject
    public SetDurationCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionFlagUtil.createDefaultRent(fileManager);
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.setduration")) {
            return "help-setduration";
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
        return builder.literal("setduration")
                .optional(KEY_DURATION, StringParser.stringParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setduration");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.setduration") && (!sender.hasPermission("areashop.setduration.landlord") && sender instanceof Player)) {
            this.messageBridge.message(sender, "setduration-noPermission");
            return;
        }
        RentRegion rent = RegionFlagUtil.getOrParseRentRegion(context, this.regionFlag);
        if (!sender.hasPermission("areashop.setduration")
                && !(sender instanceof Player player
                && rent.isLandlord(player.getUniqueId()))
        ) {
            this.messageBridge.message(sender, "setduration-noLandlord", rent);
            return;
        }
        String rawDuration = context.get(KEY_DURATION);
        if ("default".equalsIgnoreCase(rawDuration) || "reset".equalsIgnoreCase(rawDuration)) {
            rent.setDuration(null);
            rent.update();
            this.messageBridge.message(sender, "setduration-successRemoved", rent);
            return;
        }
        DurationInput duration = parseInternalDuration(rawDuration);
        rent.setDuration(duration.toTinySpacedString());
        rent.update();
        this.messageBridge.message(sender, "setduration-success", rent);
    }

    private DurationInput parseInternalDuration(String rawDuration) {
        int start = 0;
        for (int i = 0; i < rawDuration.length(); i++) {
            if (Character.isAlphabetic(rawDuration.charAt(i))) {
                start = i;
                break;
            }
        }
        String duration = rawDuration.substring(0, start - 1);
        String durationUnit = rawDuration.substring(start, duration.length() - 1);
        int durationInt;
        try {
            durationInt = Integer.parseInt(duration);
        } catch (NumberFormatException ex) {
            throw new AreaShopCommandException("setduration-wrongAmount", duration);
        }
        TimeUnit timeUnit = DurationInput.getTimeUnit(durationUnit)
                .orElseThrow(() -> new AreaShopCommandException("setduration-wrongFormat", durationUnit));

        boolean invalid = !switch (timeUnit) {
            case DAYS, HOURS, MINUTES, SECONDS -> true;
            default -> false;
        };
        if (invalid) {
            throw new AreaShopCommandException("setduration-wrongFormat", durationUnit);
        }
        return new DurationInput(durationInt, timeUnit);
    }

}
