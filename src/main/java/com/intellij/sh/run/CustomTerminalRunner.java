// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.sh.run;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory;
import org.jetbrains.plugins.terminal.TerminalView;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalWidgetListener;
import com.intellij.ui.content.Content;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.corda.intellij.models.TerminalType;
import net.corda.intellij.utils.TerminalUtils;

@AllArgsConstructor(staticName = "init")
public final class CustomTerminalRunner {
    private static final Logger LOG = Logger.getInstance(CustomTerminalRunner.class);
    private final Project myProject;
    private final TerminalType type;

    public void run(@NotNull String workingDirectory, @NotNull String command) {
        run(workingDirectory, command, type.getName());
    }

    public void run(@NotNull String workingDirectory, @NotNull String command, String title) {
        try {
            ShellTerminalWidget widget = create(workingDirectory, title);
            widget.executeCommand(command);
        } catch (IOException e) {
            LOG.warn("Cannot run command:" + command, e);
        }
    }

    private ShellTerminalWidget create(String workingDirectory, String title) {
        ShellTerminalWidget widget = TerminalUtils.findBy(type);
        if (widget == null) {
            TerminalView terminalView = TerminalView.getInstance(myProject);
            ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID);
            widget = createLocalShellWidget(terminalView, workingDirectory);
            assert window != null;
            Content content = Objects.requireNonNull(window.getContentManager().getSelectedContent());
            content.setDisplayName(title);
            content.setDescription("CustomTerminal");
            updateListener(widget, () -> window.getContentManager().getSelectedContent().setDisplayName(title));
            TerminalUtils.cache(type, widget);
        }
        return widget;
    }

    @SneakyThrows
    private ShellTerminalWidget createLocalShellWidget(TerminalView terminalView, String workingDirectory) {
        Method[] methods = TerminalView.class.getMethods();
        for (Method method : methods) {
            if (method.getName().equals("createLocalShellWidget")) {
                int parameterCount = method.getParameterCount();
                if (parameterCount == 2) {
                    return (ShellTerminalWidget) method.invoke(terminalView, workingDirectory, "");
                } else if (parameterCount == 1) {
                    return (ShellTerminalWidget) method.invoke(terminalView, workingDirectory);
                }
                return (ShellTerminalWidget) method.invoke(terminalView, workingDirectory, "", false);
            }
        }
        throw new RuntimeException("Error occurs while creating local shell widget");
    }

    private void updateListener(ShellTerminalWidget widget, Runnable updateTerminalTitle) {
        JBTerminalWidgetListener listener = widget.getListener();
        widget.setListener(new JBTerminalWidgetListener() {
            @Override
            public void onNewSession() {
                listener.onNewSession();
            }

            @Override
            public void onTerminalStarted() {
                updateTerminalTitle.run();
            }

            @Override
            public void onPreviousTabSelected() {
               listener.onPreviousTabSelected();
            }

            @Override
            public void onNextTabSelected() {
                listener.onNextTabSelected();
            }

            @Override
            public void onSessionClosed() {
                listener.onSessionClosed();
            }

            @Override
            public void showTabs() {
                listener.showTabs();
            }

            @Override
            public void moveTabRight() {
                listener.moveTabRight();
            }

            @Override
            public void moveTabLeft() {
                listener.moveTabLeft();
            }

            @Override
            public boolean canMoveTabRight() {
                return listener.canMoveTabRight();
            }

            @Override
            public boolean canMoveTabLeft() {
                return listener.canMoveTabLeft();
            }
        });
    }
}
