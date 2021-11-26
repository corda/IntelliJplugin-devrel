package net.corda.intellij.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.corda.intellij.utils.Constants;
import net.corda.intellij.inspection.linter.FlowTestLinter;
import net.corda.intellij.settings.CordaSettings;
import net.corda.intellij.utils.CordaUtils;
import net.corda.intellij.utils.TerminalUtils;

import java.io.IOException;
import java.nio.file.Paths;

final class StartupService implements StartupActivity.DumbAware {
    private static final Logger LOG = LoggerFactory.getLogger(StartupService.class);
    private static final String CORDA_PROJECT_NAME = "corda";

    @Override
    public void runActivity(@NotNull Project project) {
        ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> {
            CordaSettings settings = CordaSettings.getInstance(project);
            settings.setCordaProject("");
            if (checkCordaProject(project)) {
                settings.setCordaProject(Constants.NAME);
                ApplicationManager.getApplication().invokeLater(CordaUtils::delegateGradleImport);
                FlowTestLinter.collectAllValidPackages(project);
                closeAllCustomTerminals(project);
                ApplicationManager.getApplication().executeOnPooledThread(new RequirementChecker(project));
            }
        });
    }

    private boolean checkCordaProject(Project project) {
        String projectCwd = project.getBasePath();
        VirtualFile gradleExecFile = CordaUtils.findGradleBuildFile(projectCwd);
        if (gradleExecFile != null && gradleExecFile.exists()) {
            try {
                setJDTPref(projectCwd);
                String contents = VfsUtil.loadText(gradleExecFile);
                return contents.contains(CORDA_PROJECT_NAME);
            } catch (IOException e) {
                LOG.error("Cannot create folder for " + projectCwd, e);
            }
        }
        return false;
    }

    private void setJDTPref(String projectCwd) throws IOException {
        String settingPath = projectCwd + "/.settings";
        String filePath = settingPath + "/org.eclipse.jdt.core.prefs";
        VirtualFile file = VfsUtil.findFile(Paths.get(filePath), false);
        if (file == null || !file.exists()) {
            VirtualFile settingsFile = VfsUtil.findFile(Paths.get(settingPath), false);
            if (settingsFile == null || !settingsFile.exists()) {
                VfsUtil.createDirectories(settingPath);
            }
        }
    }

    private void closeAllCustomTerminals(@NotNull Project project) {
        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
        @NotNull Content[] contents = window.getContentManager().getContents();
        final Key<?> terminalWidgetKey = Key.findKeyByName("TerminalWidget");
        for (Content content : contents) {
            Object obj = content.getUserData(terminalWidgetKey);
            if (obj instanceof ShellTerminalWidget) {
                TerminalUtils.terminate((ShellTerminalWidget) obj);
            }
        }
    }
}
