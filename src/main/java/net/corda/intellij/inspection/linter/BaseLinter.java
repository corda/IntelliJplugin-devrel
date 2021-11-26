package net.corda.intellij.inspection.linter;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import net.corda.intellij.utils.MessageBundle;

import java.util.List;

@AllArgsConstructor
public abstract class BaseLinter {

    @NotNull InspectionManager manager;
    @NotNull PsiClass aClass;

    public abstract List<ProblemDescriptor> check();

    ProblemDescriptor createProblemDescriptor(PsiElement element, String errorKey, Object ...errorParams) {
        return manager.createProblemDescriptor(
                element,
                MessageBundle.message(errorKey, errorParams),
                false,
                ProblemHighlightType.WARNING,
                false,
                LocalQuickFix.EMPTY_ARRAY
        );
    }

    void addToList(List<ProblemDescriptor> list, ProblemDescriptor item) {
        if (item != null) {
            list.add(item);
        }
    }
}
