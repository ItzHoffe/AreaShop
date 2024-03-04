package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.commands.util.HelpProvider;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandBean;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;

import javax.annotation.Nonnull;

/**
 * An extension of {@link CommandBean} which does extra pre-processing of the commands.
 * Adapted from <a href="https://github.com/Incendo/kitchensink">KitchenSink</a>
 */
public abstract class AreashopCommandBean extends CommandBean<CommandSender> implements HelpProvider {

    private boolean requireConfirmation;

    protected void withConfirmation() {
        this.requireConfirmation = true;
    }

    @Override
    protected final @Nonnull Command.Builder<? extends CommandSender> configure(
            final @Nonnull Command.Builder<CommandSender> builder
    ) {
        return this.configureCommand(builder)
                .meta(CloudKey.of("bukkit_description", String.class), this.stringDescription())
                .meta(ConfirmationManager.META_CONFIRMATION_REQUIRED, this.requireConfirmation);
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
    protected abstract @Nonnull Command.Builder<? extends CommandSender> configureCommand(
            @Nonnull Command.Builder<CommandSender> builder
    );
}