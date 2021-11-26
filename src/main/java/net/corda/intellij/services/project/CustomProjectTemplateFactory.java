package net.corda.intellij.services.project;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import icons.IconHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.corda.intellij.utils.MessageBundle;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomProjectTemplateFactory extends ProjectTemplatesFactory {
    private final static Map<String, String> TEMPLATE_MAP;
    static {
        Map<String, String> map = new HashMap<>(2);
        map.put(
                MessageBundle.message("module.template_name1"),
                "https://github.com/corda/cordapp-template-java/archive/release-V4.zip"
        );
        map.put(
            MessageBundle.message("module.template_name2"),
            "https://github.com/corda/cordapp-template-java/archive/token-template.zip"
        );
        TEMPLATE_MAP = Collections.unmodifiableMap(map);
    }

    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] { MessageBundle.message("module.name") };
    }

    @Override
    public Icon getGroupIcon(String group) {
        return IconHelper.default_icon;
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String group, WizardContext context) {
        return TEMPLATE_MAP.entrySet().stream()
                .map(t -> CustomArchivedProjectTemplate.createTemplate(t.getKey(), t.getValue()))
                .toArray(ProjectTemplate[]::new);
    }
}
