package net.corda.intellij.modules;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import icons.IconHelper;
import org.jetbrains.annotations.NotNull;

import net.corda.intellij.utils.Constants;
import net.corda.intellij.utils.MessageBundle;

import javax.swing.*;

public class CustomModuleType extends JavaModuleType {

    public CustomModuleType() {
        super(Constants.SYSTEM_ID.getId());
    }

    public static CustomModuleType getInstance() {
        return (CustomModuleType) ModuleTypeManager.getInstance().findByID(Constants.SYSTEM_ID.getId());
    }

    @NotNull
    @Override
    public String getName() {
        return Constants.NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return MessageBundle.message("settings.vn.corda.services.project.dest");
    }

    @NotNull
    @Override
    public Icon getNodeIcon(@Deprecated boolean b) {
        return IconHelper.default_icon;
    }
}
