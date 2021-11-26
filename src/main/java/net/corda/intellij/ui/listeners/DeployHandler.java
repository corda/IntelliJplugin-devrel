package net.corda.intellij.ui.listeners;

import com.intellij.openapi.project.Project;
import net.corda.intellij.models.CommandType;
import net.corda.intellij.utils.DialogUtils;
import net.corda.intellij.utils.MessageBundle;

public class DeployHandler implements BaseHandler {

    private static final DefaultHandler defaultHandler = new DefaultHandler(CommandType.DEPLOY);
    private static final String confirmationMessage = MessageBundle.message("command.deploy.confirmation");

    public static void execute(Project project) {
        defaultHandler.perform(project);
    }

    @Override
    public void perform(Project project) {
        if (RunHandler.areNodesDeployed(project)) {
            DialogUtils.showConfirmation(confirmationMessage, () -> {
                StopHandler.stop(project);
                defaultHandler.perform(project);
            });
        } else {
            defaultHandler.perform(project);
        }
    }
}
