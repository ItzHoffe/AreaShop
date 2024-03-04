package me.wiefferink.areashop.commands.cloud;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.ArgumentParseExceptionHandler;
import me.wiefferink.areashop.commands.util.HelpRenderer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.processors.cache.GuavaCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AreashopCloudCommands {

    private static final List<Class<? extends CloudCommandBean>> COMMAND_CLASSES = List.of(
            AddCommandCloud.class,
            AddFriendCloudCommand.class,
            AddSignCloudCommand.class,
            BuyCloudCommand.class,
            DelCloudCommand.class,
            DelFriendCloudCommand.class,
            DelSignCloudCommand.class,
            FindCloudCommand.class,
            GroupAddCloudCommand.class,
            GroupDelCloudCommand.class,
            GroupInfoCloudCommand.class,
            GroupListCloudCommand.class,
            HelpCloudCommand.class,
            InfoCloudCommand.class,
            LinkSignsCloudCommand.class,
            MeCloudCommand.class,
            MessageCloudCommand.class,
            ReloadCommand.class,
            RentCloudCommand.class,
            ResellCloudCommand.class,
            SchematicEventCloudCommand.class,
            SellCloudCommand.class,
            SetDurationCloudCommand.class,
            SetLandlordCloudCommand.class,
            SetOwnerCloudCommand.class,
            SetPriceCloudCommand.class,
            SetRestoreCloudCommand.class,
            SetTeleportCloudCommand.class,
            SetTransferCloudCommand.class,
            StackCloudCommand.class,
            StopResellCloudCommand.class,
            TeleportCloudCommand.class,
            ToggleHomeCloudCommand.class,
            TransferCloudCommand.class,
            UnrentCloudCommand.class
    );

    private final MessageBridge messageBridge;

    private final Injector injector;
    private final PaperCommandManager<CommandSender> commandManager;

    private final List<CloudCommandBean> commands = new ArrayList<>();
    private HelpRenderer helpRenderer;

    @Inject
    AreashopCloudCommands(@Nonnull Injector injector, @Nonnull Plugin plugin, @Nonnull MessageBridge messageBridge) {
        this.injector = injector;
        this.commandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );
        this.messageBridge = messageBridge;
    }

    public void registerCommands() {
        this.commands.clear();
        initCommandManager();
        var builder = commandManager.commandBuilder("areashop", "as");
        for (Class<? extends CloudCommandBean> commandClass : COMMAND_CLASSES) {
            CloudCommandBean commandBean = injector.getInstance(commandClass);
            this.commands.add(commandBean);
            var configuredBuilder = commandBean.configureCommand(builder);
            this.commandManager.command(configuredBuilder);
        }
        this.helpRenderer = new HelpRenderer(this.messageBridge, this.commands);
    }

    private void initCommandManager() {
        if (this.commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.commandManager.registerBrigadier();
        }
        ExceptionController<CommandSender> exceptionController = this.commandManager.exceptionController();
        // We need to unwrap ArgumentParseException because they wrap the custom exception messages
        exceptionController.registerHandler(ArgumentParseException.class,
                ExceptionHandler.unwrappingHandler(AreaShopCommandException.class));
        exceptionController.registerHandler(CommandExecutionException.class,
                ExceptionHandler.unwrappingHandler(AreaShopCommandException.class));
        exceptionController.registerHandler(AreaShopCommandException.class,
                new ArgumentParseExceptionHandler<>(this.messageBridge));
        var confirmationConfiguration = ConfirmationConfiguration.<CommandSender>builder()
                .cache(GuavaCache.of(CacheBuilder.newBuilder().build()))
                .noPendingCommandNotifier(x -> {
                })
                .confirmationRequiredNotifier((x, y) -> {
                })
                .build();
        ConfirmationManager<CommandSender> confirmationManager = ConfirmationManager.of(confirmationConfiguration);
        commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor());
    }

    public void showHelp(@Nonnull CommandSender sender) {
        if (this.helpRenderer == null) {
            throw new IllegalStateException("Command handler not yet initialized!");
        }
        this.helpRenderer.showHelp(sender);
    }

}
