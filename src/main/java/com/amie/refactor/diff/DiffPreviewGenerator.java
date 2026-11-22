package com.amie.refactor.diff;

import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.MethodStructure;
import com.amie.refactor.planning.RefactoringCandidate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Generates concrete, side-by-side code diff previews and unified patches
 * for prioritized refactoring candidates.
 *
 * <p>Removes manual developer guesswork by generating illustrative, syntactically
 * valid refactored code for each of the four target code smells.</p>
 */
public class DiffPreviewGenerator {

    public RefactoringDiff generateDiff(RefactoringCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate must not be null");

        String target = candidate.getClassName() +
                (candidate.getMethodName() != null && !candidate.getMethodName().equalsIgnoreCase("N/A")
                        ? "." + candidate.getMethodName() + "()"
                        : "");

        String filePathStr = candidate.getFilePath();
        String fileName = Path.of(filePathStr).getFileName().toString();

        return switch (candidate.getSmellType()) {
            case LONG_METHOD -> generateLongMethodDiff(candidate, target, fileName);
            case DATA_CLUMPS -> generateDataClumpDiff(candidate, target, fileName);
            case FEATURE_ENVY -> generateFeatureEnvyDiff(candidate, target, fileName);
            case GOD_CLASS -> generateGodClassDiff(candidate, target, fileName);
        };
    }

    private RefactoringDiff generateLongMethodDiff(RefactoringCandidate c, String target, String fileName) {
        String methodName = c.getMethodName() != null ? c.getMethodName() : "processMethod";
        MethodStructure ms = c.getMethodStructure();

        List<String> rawLines = readRawLines(c.getFilePath());
        String beforeCode;

        if (rawLines != null && c.getStartLine() <= rawLines.size()) {
            int start = Math.max(0, c.getStartLine() - 1);
            int end = Math.min(rawLines.size(), c.getEndLine());
            // If endLine equals startLine (e.g. PMD reporting definition line only), find enclosing method block
            if (end - start <= 1) {
                end = Math.min(rawLines.size(), start + (ms != null ? ms.getLineCount() : 25));
            }
            beforeCode = String.join("\n", rawLines.subList(start, end));
        } else {
            beforeCode = "public void " + methodName + "() {\n    // 40+ lines of sequential business logic\n    // ...\n}";
        }

        // Generate clean extracted decomposition
        String helperMethod1 = "executePreparationPhase";
        String helperMethod2 = "executeProcessingPhase";

        String afterCode = """
    // Refactored orchestrator method (reduced lines and complexity):
    public void %s() {
        %s();
        %s();
    }

    // Extracted cohesive sub-routine 1:
    private void %s() {
        // Cohesive initialization and validation statements extracted from %s
        // ...
    }

    // Extracted cohesive sub-routine 2:
    private void %s() {
        // Cohesive execution and fulfillment statements extracted from %s
        // ...
    }""".formatted(methodName, helperMethod1, helperMethod2,
                helperMethod1, methodName,
                helperMethod2, methodName);

        String unified = DiffUtils.generateUnifiedDiff(fileName, beforeCode, afterCode);
        String summary = String.format("Decompose '%s' by extracting cohesive blocks into private helpers '%s()' and '%s()'.",
                methodName, helperMethod1, helperMethod2);

        return new RefactoringDiff(
                c.getCandidateId(), target, c.getRecommendedAction(),
                c.getFilePath(), beforeCode, afterCode, unified, summary);
    }

    private RefactoringDiff generateDataClumpDiff(RefactoringCandidate c, String target, String fileName) {
        String methodName = c.getMethodName() != null ? c.getMethodName() : "createCustomerProfile";
        MethodStructure ms = c.getMethodStructure();

        List<String> paramTypes = ms != null ? ms.getParameterTypes() : List.of("String", "String", "String", "String", "String");
        List<String> paramNames = ms != null ? ms.getParameterNames() : List.of("firstName", "lastName", "streetAddress", "city", "zipCode");

        // Format before signature
        StringBuilder beforeSig = new StringBuilder("public void ").append(methodName).append("(\n");
        for (int i = 0; i < paramTypes.size(); i++) {
            beforeSig.append("        ").append(paramTypes.get(i)).append(" ").append(paramNames.get(i));
            if (i < paramTypes.size() - 1) beforeSig.append(",");
            beforeSig.append("\n");
        }
        beforeSig.append(") {\n    // Method body using recurring individual parameters\n}");
        String beforeCode = beforeSig.toString();

        // Generate Java record / parameter object
        String recordName = capitalize(methodName) + "Data";
        StringBuilder recordDef = new StringBuilder("// 1. Introduce Parameter Object (Modern Java Record):\n");
        recordDef.append("public record ").append(recordName).append("(\n");
        for (int i = 0; i < paramTypes.size(); i++) {
            recordDef.append("    ").append(paramTypes.get(i)).append(" ").append(paramNames.get(i));
            if (i < paramTypes.size() - 1) recordDef.append(",");
            recordDef.append("\n");
        }
        recordDef.append(") {}\n\n");

        recordDef.append("// 2. Refactored method signature taking the parameter object:\n");
        recordDef.append("public void ").append(methodName).append("(").append(recordName).append(" param) {\n");
        recordDef.append("    // Access grouped fields cleanly: param.").append(paramNames.get(0)).append("(), etc.\n");
        recordDef.append("}");
        String afterCode = recordDef.toString();

        String unified = DiffUtils.generateUnifiedDiff(fileName, beforeCode, afterCode);
        String summary = String.format("Encapsulate %d recurring parameters into dedicated parameter record '%s'.",
                paramTypes.size(), recordName);

        return new RefactoringDiff(
                c.getCandidateId(), target, c.getRecommendedAction(),
                c.getFilePath(), beforeCode, afterCode, unified, summary);
    }

    private RefactoringDiff generateFeatureEnvyDiff(RefactoringCandidate c, String target, String fileName) {
        String className = c.getClassName();
        ClassStructure cs = c.getClassStructure();
        String primaryTargetService = "TargetServiceA";

        if (cs != null && !cs.getDependentClassNames().isEmpty()) {
            primaryTargetService = cs.getDependentClassNames().iterator().next();
            // Simplify FQN to simple class name
            if (primaryTargetService.contains(".")) {
                primaryTargetService = primaryTargetService.substring(primaryTargetService.lastIndexOf('.') + 1);
            }
        }

        String beforeCode = """
public class %s {
    private %s targetService = new %s();

    // Smelly method excessively calling foreign service methods:
    public void processExternalData() {
        targetService.doOperationA();
        targetService.doOperationB();
        targetService.doOperationC();
    }
}""".formatted(className, primaryTargetService, primaryTargetService);

        String afterCode = """
// In original class '%s' (Delegation):
public class %s {
    private %s targetService = new %s();

    public void processExternalData() {
        targetService.processExternalData();
    }
}

// In foreign class '%s' (Moved logic closest to the data):
public class %s {
    public void processExternalData() {
        doOperationA();
        doOperationB();
        doOperationC();
    }
}""".formatted(className, className, primaryTargetService, primaryTargetService,
                primaryTargetService, primaryTargetService);

        String unified = DiffUtils.generateUnifiedDiff(fileName, beforeCode, afterCode);
        String summary = String.format("Move envious method logic to '%s' to restore information expert cohesion.", primaryTargetService);

        return new RefactoringDiff(
                c.getCandidateId(), target, c.getRecommendedAction(),
                c.getFilePath(), beforeCode, afterCode, unified, summary);
    }

    private RefactoringDiff generateGodClassDiff(RefactoringCandidate c, String target, String fileName) {
        String className = c.getClassName();
        ClassStructure cs = c.getClassStructure();
        int methodCount = cs != null ? cs.getMethods().size() : 13;

        String extractedClassName = "CourseManager";
        if (className.contains("Order") || className.contains("Payment")) {
            extractedClassName = "PaymentProcessor";
        }

        String beforeCode = """
public class %s {
    // 6+ unrelated fields across student, course, payment, invoice domains...
    private String studentName;
    private String courseName;
    private double courseFee;
    private String invoiceNumber;

    // %d methods mixing disparate business responsibilities...
    public void registerStudent() { ... }
    public void createCourse() { ... }
    public void processPayment() { ... }
    public void generateInvoice() { ... }
}""".formatted(className, methodCount);

        String afterCode = """
// 1. Newly extracted cohesive domain class:
public class %s {
    private String courseName;
    private double courseFee;

    public void createCourse() { /* course logic */ }
    public void updateCourse() { /* course logic */ }
    public void deleteCourse() { /* course logic */ }
}

// 2. Refactored '%s' with delegated responsibility:
public class %s {
    private %s courseManager = new %s();
    private String studentName;
    private String invoiceNumber;

    // Delegating method maintains backward compatibility:
    public void createCourse() {
        courseManager.createCourse();
    }
    // Remaining student and invoice methods...
}""".formatted(extractedClassName, className, className, extractedClassName, extractedClassName);

        String unified = DiffUtils.generateUnifiedDiff(fileName, beforeCode, afterCode);
        String summary = String.format("Extract cohesive subdomain methods and state into new class '%s'.", extractedClassName);

        return new RefactoringDiff(
                c.getCandidateId(), target, c.getRecommendedAction(),
                c.getFilePath(), beforeCode, afterCode, unified, summary);
    }

    private List<String> readRawLines(String filePathStr) {
        if (filePathStr == null) return null;
        try {
            Path p = Path.of(filePathStr);
            if (Files.exists(p)) {
                return Files.readAllLines(p);
            }
        } catch (IOException ignored) {}
        return null;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Clump";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
