package net.corda.intellij.inspection.linter;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlowLinter extends BaseLinter {

    private static final String SUSPENDABLE_ANNOTATION = "co.paralleluniverse.fibers.Suspendable";
    private static final String LIST_FLOW_SESSION_CLASS_NAME = "java.util.Collection<net.corda.core.flows.FlowSession>";
    private static final String FLOW_SESSION_CLASS_NAME = "net.corda.core.flows.FlowSession";
    private static final String FINALITY_FLOW_CLASS_NAME = "FinalityFlow";
    private static final String MISSING_SUSPENDABLE_ANNOTATION = "inspection.flow.missing.annotation.suspendable";
    private static final String MISSING_TRANSACTION_VERIFICATION = "inspection.flow.missing.transaction.verification";
    private static final String MISSING_SELF_SIGNATURE = "inspection.flow.missing.self.signature";
    private static final String USE_DEFAULT_SELECTION_IN_LIST = "inspection.flow.use.default.selection.in.list";
    private static final String MISSING_SESSION_IN_FINALITY_FLOW = "inspection.flow.missing.session.in.finality.flow";
    private static final String MISMATCHED_SEND_RECEIVE_IN_FLOW_SESSION = "inspection.flow.session.mismatched.send.receive";
    private static final String FLOW_LOGIC_CLASS = "net.corda.core.flows.FlowLogic";

    public FlowLinter(@NotNull InspectionManager manager, @NotNull PsiClass aClass) {
        super(manager, aClass);
    }

    @Override
    public List<ProblemDescriptor> check() {
        List<ProblemDescriptor> result = new LinkedList<>();
        if (aClass.getSuperClass() != null && FLOW_LOGIC_CLASS.equals(aClass.getSuperClass().getQualifiedName())) {
            PsiMethod method = findCallMethod(aClass);
            if (method != null) {
                addToList(result, checkSuspendableAnnotation(method));
                String transactionVariableName = getTransactionVariableName(method);
                if (transactionVariableName != null) {
                    if (isTransactionVerificationMissing(method, transactionVariableName)) {
                        result.add(createProblemDescriptor(method.getNameIdentifier(), MISSING_TRANSACTION_VERIFICATION));
                    }
                    if (isSelfSignatureMissing(method, transactionVariableName)) {
                        result.add(createProblemDescriptor(method.getNameIdentifier(), MISSING_SELF_SIGNATURE));
                    }
                }
                addToList(result, findMissingFlowSessionInFinalityFlowProblem(method, FINALITY_FLOW_CLASS_NAME,
                        LIST_FLOW_SESSION_CLASS_NAME, MISSING_SESSION_IN_FINALITY_FLOW));
                addToList(result, checkMismatchedSendReceiveInFlowSession(method));
            }
            result.addAll(checkDefaultSelectionInList(aClass));
            PsiField[] fields = aClass.getFields();
            if (fields != null && fields.length > 0) {
                result.addAll(Arrays.stream(fields)
                        .filter(f -> !Objects.requireNonNull(f.getModifierList()).hasExplicitModifier(PsiModifier.FINAL))
                        .map(f -> createProblemDescriptor(f.getNameIdentifier(),"inspection.state.and.flow.missing.field.final", f.getName()))
                        .collect(Collectors.toList()));
            }
        }
        return result;
    }

    private PsiMethod findCallMethod(PsiClass aClass) {
        return Arrays.stream(aClass.getMethods())
                        .filter(item -> item.getName().equals("call")).findFirst()
                        .orElse(null);
    }

    private ProblemDescriptor checkSuspendableAnnotation(PsiMethod method) {
        if (method.hasAnnotation(SUSPENDABLE_ANNOTATION)) {
            return null;
        }
        return createProblemDescriptor(method.getNameIdentifier(),
                MISSING_SUSPENDABLE_ANNOTATION);
    }

    private String getTransactionVariableName(PsiMethod method) {
        PsiStatement[] statements = Objects.requireNonNull(method.getBody()).getStatements();
        for (PsiStatement statement : statements) {
            String statementText = statement.getText();
            if (statementText.contains("TransactionBuilder")) {
                String[] strings = statementText.split("=")[0].split(" ");
                return strings[strings.length - 1].trim();
            }
        }
        return null;
    }

    private boolean isTransactionVerificationMissing(PsiMethod method,
            String transactionVariableName) {
        for (PsiStatement statement : Objects.requireNonNull(method.getBody()).getStatements()) {
            if (quicklyCheckMatchedTargetElement(statement, transactionVariableName + ".verify")) {
                return false;
            }
        }
        return true;
    }

    private boolean isSelfSignatureMissing(PsiMethod method,
            String transactionVariableName) {
        for (PsiStatement statement : Objects.requireNonNull(method.getBody()).getStatements()) {
            if (quicklyCheckMatchedTargetElement(statement,"signInitialTransaction(" + transactionVariableName)) {
                return false;
            }
        }
        return true;
    }

    private List<ProblemDescriptor> checkDefaultSelectionInList(PsiClass aClass) {
        List<ProblemDescriptor> result = new LinkedList<>();
        for (PsiMethod method : aClass.getMethods()) {
            result.addAll(checkDefaultSelectionInList(Objects.requireNonNull(method.getBody()).getStatements()));
        }
        return result;
    }

    private List<ProblemDescriptor> checkDefaultSelectionInList(PsiElement[] elements) {
        List<ProblemDescriptor> result = new LinkedList<>();
        for (PsiElement statement : elements) {
            PsiElement child = statement.getFirstChild();
            if (child instanceof PsiClass) {
                PsiClass aClass = (PsiClass) child;
                result.addAll(checkDefaultSelectionInList(aClass));
            } else if (statement instanceof PsiLocalVariable) {
                if (useDefaultSelectionInList(statement)) {
                    result.add(createUseDefaultSelectionInListProblem(statement));
                }
            } else {
                result.addAll(checkDefaultSelectionInList(statement.getChildren()));
            }
        }
        return result;
    }

    private ProblemDescriptor createUseDefaultSelectionInListProblem(PsiElement element) {
        return createProblemDescriptor(element, USE_DEFAULT_SELECTION_IN_LIST);
    }

    private boolean useDefaultSelectionInList(PsiElement element) {
        return quicklyCheckMatchedTargetElement(element, "getNotaryIdentities().get(0)");
    }

    private ProblemDescriptor findMissingFlowSessionInFinalityFlowProblem(PsiMethod method, String className, String flowSessionClassName, String errorKey) {
        PsiElement finalityFlowStatement = null;
        for (PsiStatement statement : Objects.requireNonNull(method.getBody()).getStatements()) {
            if (quicklyCheckMatchedTargetElement(statement, className + "(")) {
                finalityFlowStatement = findFinalityFlowStatement(statement.getChildren(), className);
                if (isFlowSessionExisting(finalityFlowStatement, flowSessionClassName)) {
                    return null;
                }
            }
        }
        if (finalityFlowStatement != null) {
            return createProblemDescriptor(finalityFlowStatement, errorKey);
        }
        return null;
    }

    private boolean isFlowSessionExisting(PsiElement element, String flowSessionClassName) {
        if (element == null) {
            return false;
        }
        PsiReference reference = element.getReference();
        if (reference instanceof PsiExpression && matches(((PsiExpression) reference).getType(), flowSessionClassName)) {
            return true;
        } else {
            for (PsiElement child : element.getChildren()) {
                if (isFlowSessionExisting(child, flowSessionClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ProblemDescriptor checkMismatchedSendReceiveInFlowSession(PsiMethod method) {
        PsiClass correspondingClass = findCorrespondingClass();
        if (correspondingClass == null) {
            return null;
        }
        PsiMethod correspondingCallMethod = findCallMethod(correspondingClass);
        if (correspondingCallMethod == null) {
            return null;
        }
        boolean isSender = quicklyCheckMatchedTargetElement(method, "FinalityFlow(");
        if (isSender) {
            PsiElement sendAction = findFlowSessionAction(method, "send");
            PsiElement receiveAction = findFlowSessionAction(correspondingCallMethod, "receive");
            if (sendAction != null) {
                if (receiveAction == null) {
                    return createProblemDescriptor(sendAction, MISMATCHED_SEND_RECEIVE_IN_FLOW_SESSION, aClass.getName(), correspondingClass.getName());
                }
            } else if (receiveAction != null) {
                return createProblemDescriptor(receiveAction, MISMATCHED_SEND_RECEIVE_IN_FLOW_SESSION, correspondingClass.getName(), aClass.getName());
            }
        }
        return null;
    }

    private PsiClass findCorrespondingClass() {
        PsiClass[] allInnerClasses = aClass.getContainingClass().getAllInnerClasses();
        for (PsiClass psiClass : allInnerClasses) {
            if (psiClass != aClass) {
                return psiClass;
            }
        }
        return null;
    }

    private PsiElement findFlowSessionAction(PsiMethod method, String actionName) {
        for (PsiStatement statement : Objects.requireNonNull(method.getBody()).getStatements()) {
            if (quicklyCheckMatchedTargetElement(statement, "." + actionName + "(")) {
                if (isFlowSessionExisting(statement, FLOW_SESSION_CLASS_NAME)) {
                    return statement;
                }
            }
        }
        return null;
    }

    private boolean matches(PsiType type, String name) {
        if (type == null) {
            return false;
        }

        if (type.getCanonicalText().equals(name)) {
            return true;
        }

        @NotNull PsiType[] superTypes = type.getSuperTypes();
        for (PsiType superType : superTypes) {
            if (matches(superType, name)) {
                return true;
            }
        }
        return false;
    }

    private PsiElement findFinalityFlowStatement(PsiElement[] elements, String flowClassName) {
        for (PsiElement child : elements) {
            if (child instanceof PsiConstructorCall && isTargetElement(child,
                    flowClassName)) {
                return child;
            } else {
                PsiElement result = findFinalityFlowStatement(child.getChildren(), flowClassName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private boolean quicklyCheckMatchedTargetElement(PsiElement element, String condition) {
        return element.getText().replace(" ", "").contains(condition);
    }

    private boolean isTargetElement(PsiElement element, String name) {
        if (element.getText().equals(name)) {
            return true;
        }
        for (PsiElement child : element.getChildren()) {
            if (isTargetElement(child, name)) {
                return true;
            }
        }
        return false;
    }
}
