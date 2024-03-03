package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

import javax.annotation.Nonnull;

public class ArgumentParseExceptionHandler<C> implements ExceptionHandler<C, AreaShopCommandException> {

    private final MessageBridge messageBridge;

    public ArgumentParseExceptionHandler(@Nonnull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    @Override
    public void handle(@Nonnull ExceptionContext<C, AreaShopCommandException> context) {
        AreaShopCommandException parseException = context.exception();
        String key = parseException.messageKey();
        Object[] replacements = parseException.replacements();
        if (replacements.length == 0) {
            this.messageBridge.message(context.context().sender(), key);
            return;
        }
        // Pass the values as a var-args and not as a string[]
        this.messageBridge.message(context.context().sender(), key, replacements);
    }
}
