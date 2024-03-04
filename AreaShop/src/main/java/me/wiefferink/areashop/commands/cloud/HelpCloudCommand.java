package me.wiefferink.areashop.commands.cloud;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class HelpCloudCommand extends CloudCommandBean {
    private final AreashopCloudCommands commands;

    @Inject
    public HelpCloudCommand(@Nonnull AreashopCloudCommands commands) {
        this.commands = commands;
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.help")) {
            return "help-help";
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
        return builder.literal("help")
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("help");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.help")) {
            throw new AreaShopCommandException("help-noPermission");
        }
        this.commands.showHelp(sender);
    }
}
