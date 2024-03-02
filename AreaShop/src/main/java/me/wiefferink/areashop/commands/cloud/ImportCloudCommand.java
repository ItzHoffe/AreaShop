package me.wiefferink.areashop.commands.cloud;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AcceptedValuesParser;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.regions.ImportJobFactory;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public class ImportCloudCommand extends CloudCommandBean {

    private static final CloudKey<String> KEY_SOURCE = CloudKey.of("source", String.class);

    /* RegionGroup priority usage:
       0: Settings from /config.yml
       1: Settings from /worlds/<world>/config.yml
       2: Settings from /worlds/<world>/parent-regions.yml (if their priority is set it is added to this value)
     */
    private final ImportJobFactory importJobFactory;
    private final MessageBridge messageBridge;

    @Inject
    public ImportCloudCommand(@Nonnull MessageBridge messageBridge, @Nonnull ImportJobFactory importJobFactory) {
        this.messageBridge = messageBridge;
        this.importJobFactory = importJobFactory;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.@NonNull Builder<? extends CommandSender> configureCommand(Command.@NonNull Builder<CommandSender> builder) {
        withConfirmation();
        var sourceParser = AcceptedValuesParser.ofConstant(List.of("RegionForSale"), "import-wrongSource", true);
        return builder.literal("import")
                .permission("areashop.import")
                .required(KEY_SOURCE, ParserDescriptor.of(sourceParser, String.class))
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("import");
    }

    // TODO:
    //  - Landlord?
    //  - Friends
    //  - Region flags?
    //  - Settings from the 'permissions' section in RegionForSale/config.yml?

    private void handleCommand(@NonNull CommandContext<CommandSender> context) {
        CommandSender sender = context.sender();
        if (!sender.hasPermission("areashop.import")) {
            messageBridge.message(sender, "import-noPermission");
            return;
        }
        String importSource = context.get(KEY_SOURCE);
        if (!"RegionForSale".equalsIgnoreCase(importSource)) {
            throw new AreaShopCommandException("import-wrongSource");
        }
        importJobFactory.createImportJob(sender).execute();
    }

}










