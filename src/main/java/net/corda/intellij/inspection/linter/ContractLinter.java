package net.corda.intellij.inspection.linter;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import org.eclipse.aether.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ContractLinter extends BaseLinter {
    private static final List<String> CONTRACT_INTERFACES = Collections.singletonList(
            "net.corda.core.contracts.Contract"
    );
    private static final String FIELD_ID_NAME = "ID";
    private static final String CONTRACTS_COMMAND_DATA = "net.corda.core.contracts.CommandData";

    public ContractLinter(@NotNull InspectionManager manager, @NotNull PsiClass aClass) {
        super(manager, aClass);
    }

    @Override
    public List<ProblemDescriptor> check() {
        List<ProblemDescriptor> problems = new LinkedList<>();
        PsiClass[] interfaces = aClass.getInterfaces();
        if (interfaces != null && interfaces.length > 0
                && Arrays.stream(interfaces).anyMatch(i -> CONTRACT_INTERFACES.contains(i.getQualifiedName()))) {
            PsiField[] fields = aClass.getFields() != null ? aClass.getFields() : new PsiField[0];
            Optional<PsiField> fieldOpt = Arrays.stream(fields)
                    .filter(f -> f.getName().endsWith(FIELD_ID_NAME))
                    .filter(f -> {
                        PsiModifierList modifierList = Objects.requireNonNull(f.getModifierList());
                        return modifierList.hasExplicitModifier(PsiModifier.FINAL) && modifierList.hasExplicitModifier(PsiModifier.STATIC);
                    })
                    .findFirst();
            if (!fieldOpt.isPresent()) {
                problems.add(createProblemDescriptor(aClass.getNameIdentifier(),"inspection.contract.id"));
            } else {
                String value = String.valueOf(fieldOpt.get().computeConstantValue());
                PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(aClass.getContainingFile().getContainingDirectory());
                if (aPackage != null && (StringUtils.isEmpty(value)
                        || !value.startsWith(aPackage.getQualifiedName()))) {
                    problems.add(createProblemDescriptor(fieldOpt.get().getNameIdentifier(),"inspection.contract.id.diff"));
                }
            }
            PsiClass[] innerClasses = aClass.getInnerClasses();
            if (innerClasses != null && innerClasses.length > 0) {
                PsiClass commandClass = Arrays.stream(innerClasses)
                        .filter(ic -> Arrays.stream(ic.getSupers()).anyMatch(s -> CONTRACTS_COMMAND_DATA.equals(s.getQualifiedName())))
                        .findFirst().orElse(null);
                if (commandClass != null) {
                    PsiClass[] commands = commandClass.getInnerClasses();
                    if (commands != null && commands.length > 1) {
                        List<String> commandNames = Arrays.stream(commands).map(PsiClass::getName).collect(Collectors.toCollection(LinkedList::new));
                        PsiMethod[] methods = aClass.getMethods() != null ? aClass.getMethods() : new PsiMethod[0];
                        List<String> implementedCommands = Arrays.stream(methods)
                                .map(m -> m.getName().replace("verify", ""))
                                .filter(commandNames::contains).collect(Collectors.toCollection(LinkedList::new));
                        if (implementedCommands.size() < commandNames.size()) {
                            problems.add(createProblemDescriptor(
                                    commandClass.getNameIdentifier(),
                                    "inspection.contract.command.missing",
                                    commandNames.stream().filter(n -> !implementedCommands.contains(n)).collect(Collectors.joining(","))
                                ));
                        }
                    }
                }
            }
        }
        return problems;
    }
}
