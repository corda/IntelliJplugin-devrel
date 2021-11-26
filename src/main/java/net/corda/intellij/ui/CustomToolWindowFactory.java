package net.corda.intellij.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.models.CheckResult;
import net.corda.intellij.settings.CordaSettings;
import net.corda.intellij.utils.MessageBundle;

import javax.swing.*;
import java.awt.*;


public class CustomToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final String TOOL_WINDOW_ID = "Corda";
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CustomToolWindowFactory.createToolWindow(project, toolWindow, null);
    }

    public static void updateToolWindow(@NotNull Project project,  CheckResult checker) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        assert toolWindow != null;
        CustomToolWindowFactory.createToolWindow(project, toolWindow, checker);
    }

    private static void createToolWindow(@NotNull Project project, @NotNull ToolWindow toolWindow, CheckResult checker) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content;
        if (CordaSettings.getInstance(project).isCordaProject()) {
            CustomToolWindowPanel customToolWindow = new CustomToolWindowPanel(project, checker);
            content = contentFactory.createContent(customToolWindow, "", true);
        } else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(
                    ScrollPaneFactory.createScrollPane(
                            new JBLabel(MessageBundle.message("tooling.loading"))),
                    BorderLayout.CENTER);
            content = contentFactory.createContent(panel, "", true);
        }
        toolWindow.setAvailable(true, null);
        toolWindow.getContentManager().removeAllContents(true);
        toolWindow.getContentManager().addContent(content);
    }
}
