package net.corda.intellij.services;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;

import org.apache.commons.lang.StringUtils;

import net.corda.intellij.models.CheckResult;
import net.corda.intellij.utils.CommandUtils;
import net.corda.intellij.ui.CustomToolWindowFactory;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.regex.Pattern;

public class RequirementChecker implements Runnable {
    private final Project myProject;
    public RequirementChecker(Project project) {
        myProject = project;
    }

    @Override
    public void run() {
        CheckResult
            checker = new CheckResult(javaChecker(), gitChecker(), gradleChecker(), memoryChecker());
        update(checker);
    }

    private void update(CheckResult checker) {
        ApplicationManager.getApplication().invokeLater(() -> CustomToolWindowFactory.updateToolWindow(myProject, checker));
    }

    private boolean javaChecker() {
        try {
            if (SystemInfo.isWindows) {
                Pattern JAVA_PATTERN = Pattern.compile("^.*(Java|java|Jre|jre).*(1.8)");
                String javaHome = System.getenv("JAVA_HOME");
                if (StringUtils.isNotEmpty(javaHome) && JAVA_PATTERN.matcher(javaHome).lookingAt()) {
                    return true;
                }
                String path = System.getenv("PATH");
                return StringUtils.isNotEmpty(path) && JAVA_PATTERN.matcher(path).lookingAt();
            } else {
                ProcessOutput output = ExecUtil.execAndGetOutput(new GeneralCommandLine(Arrays.asList("java", "-version")));
                String pattern = "java version \"1.8";
                return parseProcessOutput(output, pattern);
            }
        } catch (ExecutionException ignore) {
            return false;
        }
    }

    private boolean parseProcessOutput(ProcessOutput output, String pattern) {
        String result = output.getStderr();
        if (StringUtils.isEmpty(result)) {
            result = output.getStdout();
        }
        if (StringUtils.isEmpty(result)) {
            return false;
        }
        return result.contains(pattern);
    }

    private boolean gitChecker() {
        try {
            ProcessOutput output = ExecUtil.execAndGetOutput(new GeneralCommandLine(Arrays.asList("git", "--version")));
            String pattern = "git version";
            return parseProcessOutput(output, pattern);
        } catch (ExecutionException ignore) {
            return false;
        }
    }
    private boolean gradleChecker() {
        try {
            String base = myProject.getBasePath();
            String command = CommandUtils
                .normalizePath(base + CommandUtils.makeGlobalGradleCommand("").substring(1));
            GeneralCommandLine commandLine = new GeneralCommandLine(Arrays.asList(command, "--version"));
            ProcessOutput output = ExecUtil.execAndGetOutput(commandLine);
            String pattern = "Gradle 5";
            return parseProcessOutput(output, pattern);
        } catch (ExecutionException ignore) {
            return false;
        }
    }

    private long memoryChecker() {
        long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalPhysicalMemorySize();
        return memorySize / 1024 / 1024 / 1024;
    }
}
