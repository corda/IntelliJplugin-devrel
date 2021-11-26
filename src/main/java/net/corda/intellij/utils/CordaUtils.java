package net.corda.intellij.utils;

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Paths;

public final class CordaUtils {
    public static final String DEFAULT_SCRIPT_NAME = "build.gradle";
    public static final String KOTLIN_DSL_SCRIPT_NAME = "build.gradle.kts";
    private static final String GRADLE_IMPORT_EXTERNAL_PROJECT = "Gradle.ImportExternalProject";

    private CordaUtils() {
    }

    public static RunAnythingProvider<String> findGradleProvider() {
        for (RunAnythingProvider<?> provider : RunAnythingProvider.EP_NAME.getExtensionList()) {
            if (StringUtils.equalsIgnoreCase("Gradle", provider.getHelpGroupTitle())) {
                return (RunAnythingProvider<String>) provider;
            }
        }
        return null;
    }

    public static VirtualFile findGradleBuildFile(String projectDir) {
        VirtualFile gradleExecFile = VfsUtil.findFile(Paths.get(projectDir, DEFAULT_SCRIPT_NAME), false);
        if (gradleExecFile == null) {
            gradleExecFile = VfsUtil.findFile(Paths.get(projectDir, KOTLIN_DSL_SCRIPT_NAME), false);
            if (gradleExecFile != null) {
                // TODO support later
                return null;
            }
        }
        return gradleExecFile;
    }

    public static void delegateGradleImport() {
        AnAction action = ActionManager.getInstance().getAction(GRADLE_IMPORT_EXTERNAL_PROJECT);
        if (action != null) {
            try {
                action.actionPerformed(AnActionEvent.createFromAnAction(
                        action,
                        null,
                        ActionPlaces.UNKNOWN,
                        DataManager.getInstance().getDataContext()
                ));
            } catch(IllegalArgumentException e) {
                // project is linked to gradle already
//                e.printStackTrace();
            }
        }
    }
}
