package net.corda.intellij.ui.listeners;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.models.TerminalType;
import net.corda.intellij.utils.MessageBundle;
import net.corda.intellij.utils.TerminalUtils;

public class StopHandler implements BaseHandler {

    private static final NotificationGroup NOTIFICATION_GROUP =
            new NotificationGroup("Custom Notification Group", NotificationDisplayType.BALLOON, true);
    private static final String START_MESSAGE = MessageBundle.message("command.stop.start.notification");
    private static final String FINISH_MESSAGE = MessageBundle.message("command.stop.finish.notification");

    public static void stop(@NotNull Project project) {
        new Thread(() -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                    ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
                    progressIndicator.setIndeterminate(true);
                    TerminalUtils.terminateBy(TerminalType.NODE_SERVERS);
                    TerminalUtils.terminateBy(TerminalType.EXPLORER_SERVER);
                    TerminalUtils.terminateBy(TerminalType.EXPLORER_CLIENT);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NOTIFICATION_GROUP.createNotification(FINISH_MESSAGE, NotificationType.INFORMATION).notify(project);
                    });
                }, START_MESSAGE, false, project);
            });
        }).start();
    }

    @Override
    public void perform(@NotNull Project project) {
        stop(project);
    }
}
