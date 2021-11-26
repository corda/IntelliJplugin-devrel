package net.corda.intellij.inspection.linter;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StateLinter extends BaseLinter {
    private static final String DESERIALIZED_CONSTRUCTOR_ANNOTATION = "net.corda.core.serialization.ContractState";
    private static final List<String> CONTRACT_STATE_INTERFACES = Arrays.asList(
            "net.corda.core.contracts.ContractState",
            "net.corda.core.contracts.DealState",
            "net.corda.core.contracts.FixableDealState",
            "net.corda.core.contracts.FungibleAsset",
            "net.corda.core.contracts.FungibleState",
            "net.corda.core.contracts.LinearState",
            "net.corda.core.contracts.OwnableState",
            "net.corda.core.contracts.QueryableState",
            "net.corda.core.contracts.SchedulableState"
    );
    public StateLinter(@NotNull InspectionManager manager, @NotNull PsiClass aClass) {
        super(manager, aClass);
    }

    @Override
    public List<ProblemDescriptor> check() {
        List<ProblemDescriptor> problems = new LinkedList<>();
        PsiClass[] interfaces = aClass.getInterfaces();
        if (interfaces != null && interfaces.length > 0
                && Arrays.stream(interfaces).anyMatch(it -> CONTRACT_STATE_INTERFACES.contains(it.getQualifiedName()))) {
            PsiAnnotation annotation = aClass.getAnnotation("net.corda.core.contracts.BelongsToContract");
            PsiIdentifier classId = aClass.getNameIdentifier();
            if (annotation == null) {
                problems.add(createProblemDescriptor(classId, "inspection.state.missing.annotation"));
            }
            // getters and final issue
            PsiField[] fields = aClass.getFields();
            PsiMethod[] methods = aClass.getMethods();
            List<String> getterNames = new ArrayList<>();
            if (methods != null && methods.length > 0) {
                for (PsiMethod method : methods) {
                    if (!PsiType.VOID.equals(method.getReturnType())) {
                        getterNames.add(method.getName().replace("get", "").toLowerCase());
                    }
                }
            }
            if (fields != null && fields.length > 0) {
                for (PsiField field : fields) {
                    String fieldName = field.getName();
                    PsiIdentifier fieldId = field.getNameIdentifier();
                    if (!getterNames.contains(field.getName().toLowerCase())) {
                        problems.add(createProblemDescriptor(fieldId, "inspection.state.missing.getter.content", aClass.getName()));
                    } else if (!Objects.requireNonNull(field.getModifierList()).hasExplicitModifier(PsiModifier.FINAL)) {
                        problems.add(createProblemDescriptor(fieldId, "inspection.state.and.flow.missing.field.final", fieldName));
                    }
                }
            }
            // constructor
            PsiMethod[] constructors = aClass.getConstructors();
            if (constructors == null || constructors.length == 0) {
                problems.add(createProblemDescriptor(classId, "inspection.state.missing.constructor", aClass.getName()));
            } else {
                PsiMethod parameterizedConstructor = null;
                PsiMethod deserializedAnnotationConstructor = null;
                for (PsiMethod constructor : constructors) {
                    if (constructor.hasParameters()) {
                        parameterizedConstructor = constructor;
                        if (constructor.getAnnotation(DESERIALIZED_CONSTRUCTOR_ANNOTATION) != null) {
                            deserializedAnnotationConstructor = constructor;
                            break;
                        }
                    }
                }
                if (parameterizedConstructor == null) {
                    problems.add(createProblemDescriptor(classId, "inspection.state.missing.constructor", aClass.getName()));
                }
                if (constructors.length > 1 && deserializedAnnotationConstructor == null) {
                    problems.add(createProblemDescriptor(classId, "inspection.state.missing.constructor.deserialization"));
                }
            }
        }
        return problems;
    }
}
