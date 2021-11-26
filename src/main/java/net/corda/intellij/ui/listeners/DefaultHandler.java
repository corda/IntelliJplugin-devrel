package net.corda.intellij.ui.listeners;

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.openapi.project.Project;
import com.intellij.sh.run.CustomTerminalRunner;
import net.corda.intellij.models.CommandType;
import net.corda.intellij.models.TerminalType;
import net.corda.intellij.utils.CommandUtils;
import net.corda.intellij.utils.CordaUtils;

import java.util.Objects;

public class DefaultHandler implements BaseHandler {

    private final CommandType type;
    private final RunAnythingProvider<String> provider;

    public DefaultHandler(CommandType type) {
        this.type = type;
        this.provider = CordaUtils.findGradleProvider();
    }

    @Override
    public void perform(Project project) {
        if (provider != null) {
            provider.execute(DataManager.getInstance().getDataContext(),
                    CommandUtils.makePluginGradleCommand(type.getCode()));
        } else {
            // Use global gradle for run terminal.
            CustomTerminalRunner.init(project, TerminalType.OTHER).run(
                    Objects.requireNonNull(project.getBasePath()),
                    CommandUtils.makeGlobalGradleCommand(type.getCode()),
                    type.getLabel());
        }
    }
}
