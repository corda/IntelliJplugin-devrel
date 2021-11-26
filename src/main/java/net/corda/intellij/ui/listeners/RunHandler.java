package net.corda.intellij.ui.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.sh.run.CustomTerminalRunner;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.models.TerminalType;
import net.corda.intellij.utils.DialogUtils;
import net.corda.intellij.utils.MessageBundle;
import net.corda.intellij.utils.TerminalUtils;

import java.io.File;
import java.util.*;

public class RunHandler implements BaseHandler {

    private static final String RUN_NODE_FILE_NAME = "runnodes";
    public static final String RUN_COMMAND = "java -jar corda.jar";
    private static final String MESSAGE_NEED_TO_DEPLOY_NODES = MessageBundle.message("command.run.need.deploy.nodes");
    private static final String MESSAGE_RUN_NODES_AGAIN = MessageBundle.message("command.run.start.nodes.again");

    public static boolean areNodesDeployed(Project project) {
        return !findNodeFolders(project).isEmpty();
    }

    public static void execute(Project project) {
        new RunHandler().perform(project);
    }

    @Override
    public void perform(@NotNull Project project) {
        List<File> nodeFolders = findNodeFolders(project);
        if (nodeFolders.isEmpty()) {
            DialogUtils.showConfirmation(MESSAGE_NEED_TO_DEPLOY_NODES, () -> DeployHandler.execute(project));
        } else {
            File nodeFolder = nodeFolders.get(0);
            if (TerminalUtils.areNodesRunning()) {
                DialogUtils.showConfirmation(MESSAGE_RUN_NODES_AGAIN, () -> {
                    StopHandler.stop(project);
                    run(project, nodeFolder);
                });
            } else {
                run(project, nodeFolder);
            }
        }
    }

    private void run(Project project, File nodeFolder) {
        List<File> files = findAllCordaJarFiles(nodeFolder);
        Iterator<File> iterator = files.iterator();
        if (iterator.hasNext()) {
            File file = iterator.next();
            CustomTerminalRunner.init(project, TerminalType.NODE_SERVERS).run(file.getAbsolutePath(), RUN_COMMAND, file.getName());
            int i = 1;
            while (iterator.hasNext()) {
                File nextFile = iterator.next();
                TerminalUtils.invokeLater(() -> ApplicationManager.getApplication().invokeLater(() -> CustomTerminalRunner.init(project, TerminalType.NODE_SERVERS).run(nextFile.getAbsolutePath(), RUN_COMMAND, nextFile.getName())), 0.5 * i++);
            }
        }
        for (File ignored : files) {
            TerminalUtils.invokeLater(() -> {

            }, 3);
        }
    }

    private List<File> findAllCordaJarFiles(File nodeFolder) {
        List<File> files = new LinkedList<>();
        for (File file : Objects.requireNonNull(nodeFolder.listFiles())) {
            if (isValidCordaJarFolder(file)) {
                files.add(file);
            }
        }
        return files;
    }

    private boolean isValidCordaJarFolder(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        for (File child : Objects.requireNonNull(file.listFiles())) {
            if (child.isFile() && child.getName().equals("corda.jar")) {
                return true;
            }
        }
        return false;
    }

    private static List<File> findNodeFolders(Project project) {
        return findNodeFolders(new File(Objects.requireNonNull(project.getBasePath())));
    }

    private static List<File> findNodeFolders(File dir) {
        if (dir.isFile()) {
            return Collections.emptyList();
        }
        if (isValidNodeFolder(dir)) {
            return Collections.singletonList(dir);
        }
        List<File> result = new LinkedList<>();
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            result.addAll(findNodeFolders(file));
        }
        return result;
    }

    private static boolean isValidNodeFolder(File dir) {
        if (dir.isDirectory() && dir.getName().equals("nodes")) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (isValidNodeCommandFile(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidNodeCommandFile(File file) {
        return file.isFile() && file.getName().startsWith(RUN_NODE_FILE_NAME);
    }
}
