package net.corda.intellij.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.corda.intellij.inspection.linter.ContractLinter;
import net.corda.intellij.inspection.linter.StateLinter;
import net.corda.intellij.inspection.linter.FlowLinter;
import net.corda.intellij.inspection.linter.FlowTestLinter;

import javax.swing.*;
import java.util.*;

public class CordaCodeInspection extends AbstractBaseJavaLocalInspectionTool {
    public final static String INSPECTION_SHORT_NAME = "CordaCode";

    @Override
    @NotNull
    public String getShortName() {
        return INSPECTION_SHORT_NAME;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        List<ProblemDescriptor> problems = new LinkedList<>();
        problems.addAll(new FlowLinter(manager, aClass).check());
        problems.addAll(new FlowTestLinter(manager, aClass).check());
        problems.addAll(new StateLinter(manager, aClass).check());
        problems.addAll(new ContractLinter(manager, aClass).check());
        if (problems.isEmpty()) {
            return super.checkClass(aClass, manager, isOnTheFly);
        }
        return problems.toArray(new ProblemDescriptor[0]);
    }


    @Nullable
    @Override
    public JComponent createOptionsPanel() {
        return new JPanel();
    }
}
