package net.corda.intellij.ui.listeners;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.models.Command;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ExecutionListener extends MouseAdapter {
    private static final int DOUBLE_CLICK = 2;
    private final RunAnythingProvider<String> provider;
    private final Project myProject;

    public ExecutionListener(@NotNull Project project, RunAnythingProvider<String> _provider) {
        myProject = project;
        provider = _provider;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= DOUBLE_CLICK) {
            JBList<Command> commandList = (JBList<Command>) e.getSource();
            int index = commandList.locationToIndex(e.getPoint());
            Command cmd = commandList.getModel().getElementAt(index);
            cmd.getHandler().perform(myProject);
        }
    }
}
