package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.exception.handling.ExceptionHandler;

import java.util.Arrays;

public class ArgumentParseExceptionHandler<C> implements ExceptionHandler<C, GenericArgumentParseException> {

    private final MessageBridge messageBridge;

    public ArgumentParseExceptionHandler(@NonNull MessageBridge messageBridge) {
        this.messageBridge = messageBridge;
    }

    @Override
    public void handle(@NonNull ExceptionContext<C, GenericArgumentParseException> context) throws Throwable {
        GenericArgumentParseException parseException = context.exception();
        String key = parseException.errorCaption().key();
        CaptionVariable[] variables = parseException.captionVariables();
        if (variables.length == 0) {
            this.messageBridge.message(context.context().sender(), key);
            return;
        }
        String[] values = Arrays.stream(variables).map(CaptionVariable::value).toArray(String[]::new);
        // Pass the values as a var-args and not as a string[]
        this.messageBridge.message(context.context().sender(), key, (Object[]) values);
    }
}
