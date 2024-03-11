package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

import javax.annotation.Nonnull;

public class InvalidCommandSenderHandler implements ExceptionHandler<CommandSender, InvalidCommandSenderException> {

    private final MessageBridge messageBridge;

    public InvalidCommandSenderHandler(@Nonnull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    @Override
    public void handle(@NonNull ExceptionContext<CommandSender, InvalidCommandSenderException> context) throws Throwable {
        InvalidCommandSenderException exception = context.exception();
        if (exception.requiredSender().equals(Player.class)) {
            this.messageBridge.message(exception.commandSender(), "cmd-onlyByPlayer");
            return;
        }
        throw exception;
    }
}
