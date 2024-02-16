package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.ArgumentParseExceptionHandler;
import me.wiefferink.areashop.commands.util.GenericArgumentParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

@Singleton
public class AreaShopCloudCommands {

    private final PaperCommandManager<CommandSender> commandManager;

    @Inject
    public AreaShopCloudCommands(@NonNull Plugin plugin, @NonNull MessageBridge messageBridge) {
        this.commandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );
        ExceptionController<CommandSender> exceptionController = this.commandManager.exceptionController();
        exceptionController.registerHandler(GenericArgumentParseException.class,
                new ArgumentParseExceptionHandler<>(messageBridge));
        if (this.commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.commandManager.registerBrigadier();
        }
    }


}
