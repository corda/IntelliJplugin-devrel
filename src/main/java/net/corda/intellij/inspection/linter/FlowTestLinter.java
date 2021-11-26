package net.corda.intellij.inspection.linter;

import com.google.common.collect.Sets;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class FlowTestLinter extends BaseLinter {

    private static final String SETUP_METHOD_ANNOTATION = "org.junit.Before";
    private static final String FIND_CORDAPP_KEYWORD = "TestCordapp.findCordapp";
    private static final String TEST_INCORRECT_PACKAGE_IS_USED = "inspection.flow.test.incorrect.package.is.used";
    private static final Set<String> VALID_PACKAGES = new HashSet<>();

    public FlowTestLinter(@NotNull InspectionManager manager, @NotNull PsiClass aClass) {
        super(manager, aClass);
    }

    public static void collectAllValidPackages(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VALID_PACKAGES.clear();
            VALID_PACKAGES.addAll(getPackages(
                    new File(Objects.requireNonNull(project.getBasePath())), PsiManager.getInstance(project)));
        });
    }

    private static Set<String> getPackages(File file, PsiManager manager) {
        if (file.isFile()) {
            VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, false);
            if (virtualFile == null) {
                return Collections.emptySet();
            }
            PsiFile psiFile = manager.findFile(virtualFile);
            if (psiFile instanceof PsiJavaFile) {
                return Sets.newHashSet(((PsiJavaFile) psiFile).getPackageName());
            }
            return Collections.emptySet();
        }
        Set<String> result = Sets.newHashSet();
        for (File child : file.listFiles()) {
            result.addAll(getPackages(child, manager));
        }
        return result;
    }

    @Override
    public List<ProblemDescriptor> check() {
        List<ProblemDescriptor> result = new LinkedList<>();
        String qualifiedName = aClass.getQualifiedName();
        if (qualifiedName != null && qualifiedName.toLowerCase().contains("test")) {
            PsiMethod method = Arrays.stream(aClass.getMethods())
                    .filter(item -> item.hasAnnotation(SETUP_METHOD_ANNOTATION))
                    .findFirst()
                    .orElse(null);
            if (method != null) {
                addToList(result, findWrongAppPackageSettingStatement(method));
            }
        }
        return result;
    }

    private ProblemDescriptor findWrongAppPackageSettingStatement(PsiMethod method) {
        for (PsiStatement statement : method.getBody().getStatements()) {
            List<PsiElement> elements = findAppPackageSettingStatement(statement.getChildren());
            for (PsiElement element : elements) {
                String appPackage = element.getChildren()[1].getChildren()[1].getText();
                // remove double quotes.
                String adjustedAppPackage = appPackage.substring(1, appPackage.length() - 1).trim();
                if (!VALID_PACKAGES.contains(adjustedAppPackage)) {
                    return createProblemDescriptor(statement, TEST_INCORRECT_PACKAGE_IS_USED);
                }
            }
        }
        return null;
    }

    private List<PsiElement> findAppPackageSettingStatement(PsiElement[] elements) {
        List<PsiElement> result = new LinkedList<>();
        for (PsiElement child : elements) {
            if (isAppPackageSettingElement(child)) {
                result.add(child);
            } else {
                result.addAll(findAppPackageSettingStatement(child.getChildren()));
            }
        }
        return result;
    }

    private boolean isAppPackageSettingElement(PsiElement element) {
        if (element == null) {
            return false;
        }
        for (PsiElement child : element.getChildren()) {
            if (child.getText().equals(FIND_CORDAPP_KEYWORD)) {
                return true;
            }
        }
        return false;
    }
}
