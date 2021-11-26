package net.corda.intellij.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import net.corda.intellij.models.CheckResult;
import net.corda.intellij.models.Command;
import net.corda.intellij.models.CommandType;
import net.corda.intellij.ui.listeners.CleanHandler;
import net.corda.intellij.ui.listeners.CodeInspectionHandler;
import net.corda.intellij.ui.listeners.DefaultHandler;
import net.corda.intellij.ui.listeners.DeployHandler;
import net.corda.intellij.ui.listeners.ExecutionListener;
import net.corda.intellij.ui.listeners.RunHandler;
import net.corda.intellij.ui.listeners.ShowExplorerHandler;
import net.corda.intellij.ui.listeners.StopHandler;
import net.corda.intellij.utils.CordaUtils;
import net.corda.intellij.utils.MessageBundle;
import net.miginfocom.swing.MigLayout;

public class CustomToolWindowPanel extends JPanel {
    private JList<Command> commandList;

    public CustomToolWindowPanel(Project project, CheckResult checker) {
        initializeCommandList(project);
        setLayout(new MigLayout("wrap 1, insets 0 4 4 4", "[grow,fill]",""));
        setBackground(JBColor.WHITE);
        add(commandList);
        add(createPreTitle());
        add(createRequirementPanel(checker));
    }

    private JPanel createPreTitle() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JBLabel requirementTitle = new JBLabel(MessageBundle.message("requirement.prerequisite.text"));
        Font f = requirementTitle.getFont();
        requirementTitle.setFont(new Font(f.getName(), Font.BOLD, f.getSize() + 3));
        requirementTitle.setHorizontalAlignment(SwingConstants.CENTER);
        jPanel.add(requirementTitle, BorderLayout.CENTER);
        jPanel.setBorder(new MatteBorder(1, 0, 0, 0, JBColor.BLACK));
        jPanel.setBackground(JBColor.WHITE);
        return jPanel;
    }

    private JPanel createRequirementPanel(CheckResult checker) {
        JPanel row1 = new JPanel(new GridLayout(2, 2, 1, 0));
        row1.add(createRequirementItem(
                "Java (Required)",
                "== 1.8",
                checker != null && checker.isJavaCheck(),
                checker == null));
        long memory = checker != null ? checker.getMemory() : -1;
        String memoryText = "";
        if (memory >= 15 && memory <= 18) {
            memoryText = " (16GB)";
        } else if (memory > -1) {
            memoryText = " (" + memory + ")";
        }
        row1.add(createRequirementItem(
                "Memory" + memoryText,
                ">= 8GB",
                checker != null && checker.getMemory() > 8,
                checker == null));
        row1.add(createRequirementItem(
                "Git (Optional)",
                "Any",
                checker != null && checker.isGitCheck(),
                checker == null));
        row1.add(createRequirementItem(
                "Gradle (Optional)",
                "between 5.1 and 5.6.4",
                checker != null && checker.isGradleCheck(),
                checker == null));
        return row1;
    }

    private JPanel createRequirementItem(String name, String requirement, boolean status, boolean isLoading) {
        JPanel jp = new JPanel();
        jp.setBackground(JBColor.WHITE);
        jp.setBorder(BorderFactory.createLineBorder(JBColor.LIGHT_GRAY));
        jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
        JBLabel lblName = new JBLabel(name);
        Font f = lblName.getFont();
        lblName.setFont(new Font(f.getName(), Font.BOLD, f.getSize() + 2));
        jp.add(lblName);
        JBLabel lblStatus = new JBLabel(MessageBundle.message("requirement.loading"));
        lblStatus.setFont(f.deriveFont(f.getStyle() | Font.ITALIC));
        if (!isLoading) {
            lblStatus.setText(status ? MessageBundle.message("requirement.valid"): MessageBundle.message("requirement.invalid"));
            lblStatus.setForeground(status ? JBColor.green : JBColor.red);
            lblStatus.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
        }
        jp.add(lblStatus);
        jp.add(new JBLabel("-"));
        if (name.startsWith("Memory")) {
            jp.add(new JBLabel(MessageBundle.message("requirement.operation")));
        } else if (name.startsWith("Java")){
            jp.add(new JBLabel(MessageBundle.message("requirement.label")));
        } else {
            jp.add(new JBLabel(MessageBundle.message("requirement.label.optional")));
        }
        jp.add(new JBLabel(requirement));
        jp.add(new JBLabel("  "));
        return jp;
    }

    private void initializeCommandList(Project project) {
        commandList = new JBList<>(makeCommandModel());
        commandList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        commandList.setCellRenderer(new CommandRenderer());
        commandList.addMouseListener(new ExecutionListener(project, CordaUtils.findGradleProvider()));
    }

    @NotNull
    private DefaultListModel<Command> makeCommandModel() {
        List<Command> COMMANDS = Arrays.asList(
                new Command(CommandType.CLEAN, new CleanHandler()),
                new Command(CommandType.TEST, new DefaultHandler(CommandType.TEST)),
                new Command(CommandType.BUILD, new DefaultHandler(CommandType.BUILD)),
                new Command(CommandType.DEPLOY, new DeployHandler()),
                new Command(CommandType.RUN, new RunHandler()),
                new Command(CommandType.STOP, new StopHandler()),
                new Command(CommandType.SHOW, new ShowExplorerHandler()),
                new Command(CommandType.INSPECT, new CodeInspectionHandler()));
        DefaultListModel<Command> listModel = new DefaultListModel<>();
        for (Command cmd : COMMANDS) {
            listModel.addElement(cmd);
        }
        return listModel;
    }

    private static class CommandRenderer extends JLabel implements ListCellRenderer<Command> {
        public CommandRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends Command> list, Command cmd, int index, boolean isSelected, boolean cellHasFocus) {
            String code = cmd.getType().getCode();
            URL resource = getClass().getResource("/icons/" + code + ".png");
            if (resource != null) {
                ImageIcon imageIcon = new ImageIcon(resource);
                setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
            }
            setText(cmd.getType().getLabel());
            setToolTipText(MessageBundle.message("tooling.action.tooltip", code));
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }
}
