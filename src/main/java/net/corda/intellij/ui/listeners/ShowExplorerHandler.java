package net.corda.intellij.ui.listeners;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.sh.run.CustomTerminalRunner;
import net.corda.intellij.models.NodeClientData;
import net.corda.intellij.models.TerminalType;
import net.corda.intellij.utils.CommandUtils;
import net.corda.intellij.utils.DialogUtils;
import net.corda.intellij.utils.HttpUtils;
import net.corda.intellij.utils.MessageBundle;
import net.corda.intellij.utils.PortConstants;
import net.corda.intellij.utils.TerminalUtils;
import net.lingala.zip4j.ZipFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ShowExplorerHandler implements BaseHandler {
    private static final String EXPLORER_SERVER_FILE_NAME = "explorer-server-0.1.0.jar";
    private static final String NODE_EXPLORER_FILE_NAME = "node-explorer.zip";
    private static final String MESSAGE_DEPLOY_NODES_OR_USE_REMOTE_NODE = MessageBundle.message("command.run.need.deploy.nodes.or.use.remote.nodes");
    private static final String MESSAGE_RUN_NODES_OR_USE_REMOTE_NODE = MessageBundle.message("command.run.need.run.nodes.or.use.remote.nodes");
    private static final String MESSAGE_RUN_LOCAL_NODES = MessageBundle.message("command.run.need.run.nodes");
    private static final String MESSAGE_USE_REMOTE_NODES = MessageBundle.message("command.run.use.remote.nodes");
    private static final String MESSAGE_REQUIRED_NODEJS_TITLE = MessageBundle.message("command.run.require.nodejs.title");
    private static final String MESSAGE_REQUIRED_NODEJS_MESSAGE = MessageBundle.message("command.run.require.nodejs.message");
    private final String pluginWorkingDirectory;
    private boolean isNodeAvailable;

    public ShowExplorerHandler() {
        File pluginDirectoryPath = Objects.requireNonNull(PluginManager.getPlugin(PluginId.findId("org.jetbrains.corda"))).getPath();
        this.pluginWorkingDirectory = pluginDirectoryPath.getAbsolutePath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            this.isNodeAvailable = isNodeAvailable();
        });
        copyNodeExplorer();
    }

    @Override
    public void perform(@NotNull Project project) {
        if (!isNodeAvailable) {
            Messages.showInfoMessage(project, MESSAGE_REQUIRED_NODEJS_MESSAGE, MESSAGE_REQUIRED_NODEJS_TITLE);
        } else if (!RunHandler.areNodesDeployed(project)) {
            DialogUtils.showConfirmation(MESSAGE_DEPLOY_NODES_OR_USE_REMOTE_NODE, () -> {
                perform(project, true);
            });
        } else if (!TerminalUtils.areNodesRunning()) {
            DialogUtils.showConfirmation(MESSAGE_RUN_NODES_OR_USE_REMOTE_NODE,
                    MESSAGE_RUN_LOCAL_NODES,
                    () -> {
                        RunHandler.execute(project);
                        TerminalUtils.invokeLater(() -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                perform(project, false);
                            });
                        }, 10);
                    },
                    MESSAGE_USE_REMOTE_NODES,
                    () -> {
                        perform(project, true);
                    });
        } else {
            perform(project, false);
        }
    }

    private void perform(Project project, boolean useRemoteLogin) {
        String token = UUID.randomUUID().toString();
        String command = "java -jar " + EXPLORER_SERVER_FILE_NAME + " --servertoken=" + token;
        TerminalUtils.invokeLater(() -> {
            ApplicationManager.getApplication().invokeLater(() -> CustomTerminalRunner.init(project, TerminalType.EXPLORER_SERVER)
                    .run(getNodeExploreDirectory(), command));
            HttpUtils.waitUntilApiIsReady("localhost", PortConstants.NODE_EXPLORER_SERVER_PORT, () -> {
                loadNodeExplorerClient(project);
                HttpUtils
                    .waitUntilApiIsReady("localhost", PortConstants.NODE_EXPLORER_CLIENT_PORT, () -> {
                    createNodeExplorerClientContent(project, token, useRemoteLogin);
                    TerminalUtils.invokeLater(() -> {
                        BrowserUtil.open("http://localhost:" + PortConstants.NODE_EXPLORER_CLIENT_PORT);
                    }, 1);
                });
            });
        }, 1);
    }

    private void copyNodeExplorer() {
        String nodeExplorerDeploymentPath = getNodeExplorerDeploymentPath();
        File explorerServerFile = new File(nodeExplorerDeploymentPath);
        if (!explorerServerFile.exists()) {
            try {
                Files.copy(getClass().getResourceAsStream("/node-explorer/" + NODE_EXPLORER_FILE_NAME), explorerServerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                ZipFile zipFile = new ZipFile(new File(nodeExplorerDeploymentPath));
                zipFile.extractAll(this.pluginWorkingDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadNodeExplorerClient(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            CustomTerminalRunner.init(project, TerminalType.EXPLORER_CLIENT).run(getNodeWebViewDirectory(), "node server.js --port=" + PortConstants.NODE_EXPLORER_CLIENT_PORT);
        });
    }

    private void createNodeExplorerClientContent(Project project, String clientToken, boolean useRemoteLogin) {
        Optional<NodeClientData> optionalData = findAllGradleFiles(new File(Objects.requireNonNull(project.getBasePath()))).stream().map(item -> HttpUtils
            .get("http://localhost:" + PortConstants.NODE_EXPLORER_CLIENT_PORT + "/parseGradleFile?path=" + item, NodeClientData.class)).filter(item -> item.isValid()).findFirst();
        if (optionalData.isPresent()) {
            String templateFilePath = CommandUtils.normalizePath(pluginWorkingDirectory + "/node-explorer/view/dist/index.html");
            VirtualFile templateFile = VfsUtil.findFile(Paths.get(templateFilePath), false);
            assert templateFile != null;
            try {
                String content = VfsUtil.loadText(templateFile);
                NodeClientData data = optionalData.get();
                content = content.replace("T_NODE_DEFAULTS", data.getNodeDefaults())
                        .replace("T_NODE_LIST", data.getActiveNodeConfigs())
                        .replace("T_ARE_NODES_RUNNING", String.valueOf(data.isNodesRunning()))
                        .replace("T_CLIENT_TOKEN", clientToken)
                        .replace("T_REMOTE_LOGIN", String.valueOf(useRemoteLogin));
                String indexFilePath = CommandUtils.normalizePath(this.pluginWorkingDirectory + "/node-explorer/view/dist/index.html");
                Files.write(new File(indexFilePath).toPath(), content.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new RuntimeException("Something went wrong", e);
            }
        }
    }

    private List<String> findAllGradleFiles(File file) {
        if (file.isFile()) {
            if (file.getName().equalsIgnoreCase("build.gradle")) {
                return Collections.singletonList(file.getAbsolutePath().replace("\\", "/"));
            }
            return Collections.emptyList();
        }
        List<String> result = new LinkedList<>();
        for (File childFile : file.listFiles()) {
            result.addAll(findAllGradleFiles(childFile));
        }
        return result;
    }

    private String getNodeExploreDirectory() {
        return pluginWorkingDirectory + CommandUtils.normalizePath("/node-explorer");
    }

    private String getNodeExplorerDeploymentPath() {
        return pluginWorkingDirectory + CommandUtils.normalizePath("/" + NODE_EXPLORER_FILE_NAME);
    }

    private String getNodeWebViewDirectory() {
        return pluginWorkingDirectory + CommandUtils.normalizePath("/node-explorer/view");
    }

    private boolean isNodeAvailable() {
        try {
            ProcessOutput output = ExecUtil.execAndGetOutput(new GeneralCommandLine(Arrays.asList("node", "--version")));
            return output.getExitCode() == 0;
        } catch (ExecutionException ignore) {
            return false;
        }
    }
}
