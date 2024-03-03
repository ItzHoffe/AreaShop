package me.wiefferink.areashop.adapters.plugins.essentials;

import me.wiefferink.areashop.features.homeaccess.AccessControlValidator;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;


public interface HomeListenerFactory {

    @Nonnull HomeModificationListener createListener(@Nonnull AccessControlValidator validator);

}
