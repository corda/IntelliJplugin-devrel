package net.corda.intellij.utils;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;


public class MessageBundle extends AbstractBundle {
    @NonNls public static final String PATH_TO_BUNDLE = "messages.MessageBundle";
    private static final MessageBundle BUNDLE = new MessageBundle();

    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, Object @NotNull ... params) {
        return BUNDLE.getMessage(key, params);
    }

    public MessageBundle() {
        super(PATH_TO_BUNDLE);
    }
}
