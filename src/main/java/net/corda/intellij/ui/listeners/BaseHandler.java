package net.corda.intellij.ui.listeners;

import com.intellij.openapi.project.Project;

public interface BaseHandler {
    void perform(Project project);
}
