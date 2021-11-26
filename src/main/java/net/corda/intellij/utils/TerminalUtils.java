package net.corda.intellij.utils;

import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import net.corda.intellij.models.TerminalType;

import java.util.*;

public final class TerminalUtils {

    private static final Map<TerminalType, Set<ShellTerminalWidget>> WIDGETS = new HashMap<>();

    public static void cache(TerminalType type, ShellTerminalWidget widget) {
        if (type.isCacheable()) {
            if (!WIDGETS.containsKey(type)) {
                WIDGETS.put(type, new HashSet<>());
            }
            WIDGETS.get(type).add(widget);
        }
    }

    public static ShellTerminalWidget findBy(TerminalType type) {
        Set<ShellTerminalWidget> widgets = WIDGETS.getOrDefault(type, Collections.emptySet());
        if (widgets.isEmpty()) {
            return null;
        }
        if (type.isSingleton()) {
            return widgets.iterator().next();
        }
        return null;
    }

    public static void terminateBy(TerminalType type) {
        if (WIDGETS.containsKey(type)) {
            Set<ShellTerminalWidget> widgets = WIDGETS.remove(type);
            for (ShellTerminalWidget widget : widgets) {
                terminate(widget);
            }
        }
    }

    public static void terminate(ShellTerminalWidget widget) {
        widget.dispose();
        while (widget.isSessionRunning()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean areNodesRunning() {
        return WIDGETS.containsKey(TerminalType.NODE_SERVERS);
    }

    public static void invokeLater(Runnable runnable, double timeInSecond) {
        new Thread(() -> {
            try {
                double sleepTime = timeInSecond * 1000;
                Thread.sleep((int) sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runnable.run();
        }).start();
    }
}
