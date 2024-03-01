package me.wiefferink.areashop.commands.cloud;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.wiefferink.areashop.commands.CloudCommandBean;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;

import java.util.List;
import java.util.function.Function;

@Singleton
public class AreashopCloudCommands {

    private static final List<Class<? extends CloudCommandBean>> COMMAND_CLASSES = List.of(
            AddCommandCloud.class,
            AddFriendCloudCommand.class,
            AddSignCloudCommand.class
    );

    private final Injector injector;
    private final PaperCommandManager<CommandSender> commandManager;

    @Inject
    AreashopCloudCommands(@NonNull Injector injector, @NonNull Plugin plugin) {
        this.injector = injector;
        this.commandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );
    }

    public void registerCommands() {
        initCommandManager(this.commandManager);
        for (Class<? extends CloudCommandBean> commandClass : COMMAND_CLASSES) {
            CloudCommandBean commandBean = injector.getInstance(commandClass);
            commandManager.command(commandBean);
        }
    }

    private static void initCommandManager(PaperCommandManager<?> commandManager) {
        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        }
    }



}
