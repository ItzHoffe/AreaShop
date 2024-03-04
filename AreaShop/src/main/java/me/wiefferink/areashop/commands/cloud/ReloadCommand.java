package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;

import javax.annotation.Nonnull;

@Singleton
public class ReloadCommand extends CloudCommandBean {
    private final AreaShop plugin;

    @Inject
    public ReloadCommand(@Nonnull AreaShop plugin) {
        this.plugin = plugin;
    }


    @Override
    public String stringDescription() {
        return null;
    }

    @Nonnull
    @Override
    protected Command.Builder<? extends CommandSender> configureCommand(@Nonnull Command.Builder<CommandSender> builder) {
        return builder.literal("reload")
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("reload");
    }


    public String getHelp(CommandSender target) {
        if (target.hasPermission("areashop.reload")) {
            return "help-reload";
        }
        return null;
    }

    private void handleCommand(@Nonnull CommandContext<CommandSender> context) {
        if (!context.hasPermission("areashop.reload")) {
            throw new AreaShopCommandException("reload-noPermission");
        }
        this.plugin.reload(context.sender());
    }
}
