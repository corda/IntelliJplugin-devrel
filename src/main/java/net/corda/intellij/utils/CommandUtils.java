package net.corda.intellij.utils;

import com.intellij.openapi.util.SystemInfo;


public final class CommandUtils {

    private CommandUtils() {
    }

    public static String makeGlobalGradleCommand(String task) {
        final String executeGradle = SystemInfo.isWindows ? ".\\gradlew.bat" : "./gradlew";
        return makeGradleCommand(executeGradle,  task);
    }

    public static String makePluginGradleCommand(String task) {
        return makeGradleCommand("gradle", task);
    }

    private static String makeGradleCommand(String executeGradle, String task) {
        return executeGradle + " " + task;
    }

    public static String normalizePath(String path) {
        return SystemInfo.isWindows ? path.replace("/", "\\") : path;
    }
}
