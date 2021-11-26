package net.corda.intellij.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.corda.intellij.utils.MessageBundle;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor @Getter
public enum TerminalType {
    NODE_SERVERS(true, false),
    EXPLORER_SERVER(true, true),
    EXPLORER_CLIENT(true, true),
    OTHER(false, false);

    private final boolean isCacheable;
    private final boolean isSingleton;

    public static final List<String> TERMINAL_TAB_NAMES = Arrays.asList(NODE_SERVERS.getName(), EXPLORER_SERVER.getName(), EXPLORER_CLIENT.getName());

    public String getName() {
        return MessageBundle.message(TerminalType.class.getSimpleName() + "." + name());
    }
}
