package me.wiefferink.areashop.commands.cloud;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.ArgumentParseExceptionHandler;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.processors.cache.CloudCache;
import org.incendo.cloud.processors.cache.GuavaCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;

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
            RentCloudCommand.class
    );

    private final MessageBridge messageBridge;

    private final Injector injector;
    private final PaperCommandManager<CommandSender> commandManager;

    @Inject
    AreashopCloudCommands(@NonNull Injector injector, @NonNull Plugin plugin, @NonNull MessageBridge messageBridge) {
        this.injector = injector;
        this.commandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );
        this.messageBridge = messageBridge;
    }

    public void registerCommands() {
        initCommandManager();
        var builder = commandManager.commandBuilder("areashop", "as");
        for (Class<? extends CloudCommandBean> commandClass : COMMAND_CLASSES) {
            CloudCommandBean commandBean = injector.getInstance(commandClass);
            var configuredBuilder = commandBean.configureCommand(builder);
            this.commandManager.command(configuredBuilder);
        }
    }

    private void initCommandManager() {
        if (this.commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.commandManager.registerBrigadier();
        }
        ExceptionController<CommandSender> exceptionController = this.commandManager.exceptionController();
        // We need to unwrap ArgumentParseException because they wrap the custom exception messages
        exceptionController.registerHandler(ArgumentParseException.class, ExceptionHandler.unwrappingHandler(AreaShopCommandException.class));
        exceptionController.registerHandler(AreaShopCommandException.class,
                new ArgumentParseExceptionHandler<>(this.messageBridge));
        var confirmationConfiguration = ConfirmationConfiguration.<CommandSender>builder()
                .cache(GuavaCache.of(CacheBuilder.newBuilder().build()))
                .noPendingCommandNotifier(x -> {})
                .confirmationRequiredNotifier((x, y) -> {})
                .build();
        ConfirmationManager<CommandSender> confirmationManager = ConfirmationManager.of(confirmationConfiguration);
        commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor());
    }



}
