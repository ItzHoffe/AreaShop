package me.wiefferink.areashop.commands.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.parsing.ParserException;

public class GenericArgumentParseException extends ParserException {

    public GenericArgumentParseException(@Nullable Throwable cause,
                                            @NonNull Class<?> argumentParser,
                                            @NonNull CommandContext<?> context,
                                            @NonNull Caption errorCaption,
                                            @NonNull CaptionVariable... captionVariables) {
        super(cause, argumentParser, context, errorCaption, captionVariables);
    }

    public GenericArgumentParseException(@NonNull Class<?> argumentParser,
                                            @NonNull CommandContext<?> context,
                                            @NonNull Caption errorCaption,
                                            @NonNull CaptionVariable... captionVariables) {
        super(argumentParser, context, errorCaption, captionVariables);
    }
}
