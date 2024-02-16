package me.wiefferink.areashop.commands;

import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandBean;
import org.incendo.cloud.key.CloudKey;

/**
 * An extension of {@link CommandBean} which does extra pre-processing of the commands.
 * Adapted from <a href="https://github.com/Incendo/kitchensink">KitchenSink</a>
 */
public abstract class CloudCommandBean extends CommandBean<CommandSender> {

    @Override
    protected final Command.@NonNull Builder<? extends CommandSender> configure(
            final Command.@NonNull Builder<CommandSender> builder
    ) {
        return this.configureCommand(builder)
                .meta(CloudKey.of("bukkit_description", String.class), this.stringDescription());
    }

    /**
     * Returns a simple string description of this command.
     *
     * <p>This is primarily used in the platform-native help menus.</p>
     *
     * @return command description
     */
    public abstract String stringDescription();

    /**
     * Configures the command and returns the updated builder.
     *
     * @param builder builder to configure
     * @return the updated builder
     */
    protected abstract Command.@NonNull Builder<? extends CommandSender> configureCommand(
            Command.@NonNull Builder<CommandSender> builder
    );
}