package net.corda.intellij.services.project;

import com.intellij.openapi.module.ModuleType;
import com.intellij.platform.templates.ArchivedProjectTemplate;
import com.intellij.util.io.HttpRequests;
import icons.IconHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.corda.intellij.modules.CustomModuleType;
import net.corda.intellij.utils.MessageBundle;

import javax.swing.*;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class CustomArchivedProjectTemplate extends ArchivedProjectTemplate {
    private final String myTemplatePath;

    public CustomArchivedProjectTemplate(@NotNull String displayName, @NotNull String templatePath) {
        super(displayName, "true");
        myTemplatePath = templatePath;
    }

    @Override
    protected ModuleType<?> getModuleType() {
        return CustomModuleType.getInstance();
    }

    @Override
    public <T> T processStream(@NotNull ArchivedProjectTemplate.StreamProcessor<T> consumer) throws IOException {
        return HttpRequests.request(myTemplatePath).connect(request -> {
            try (ZipInputStream zip = new ZipInputStream(request.getInputStream())) {
                return consumer.consume(zip);
            }
        });
    }

    @Nullable
    @Override
    public String getDescription() {
        return MessageBundle.message("module.description");
    }

    @Override
    public Icon getIcon() {
        return IconHelper.default_icon;
    }

    static CustomArchivedProjectTemplate createTemplate(String name, String url) {
        return new CustomArchivedProjectTemplate(name, url);
    }
}
