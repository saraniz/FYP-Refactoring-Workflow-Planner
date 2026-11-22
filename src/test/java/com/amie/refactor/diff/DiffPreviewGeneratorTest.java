package com.amie.refactor.diff;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.MethodStructure;
import com.amie.refactor.planning.RefactoringAction;
import com.amie.refactor.planning.RefactoringCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DiffPreviewGeneratorTest {

    private final DiffPreviewGenerator generator = new DiffPreviewGenerator();

    @Test
    void testLongMethodDiffGeneration() {
        CodeSmell smell = new CodeSmell(
                SmellType.LONG_METHOD, "LongMethodExample", "demo.LongMethodExample",
                "processOrder", "sample-projects/smell-test-project1/src/main/java/demo/LongMethodExample.java",
                5, 47, "3", "NcssCount", "NCSS count of 21");
        MethodStructure ms = new MethodStructure(
                "demo.LongMethodExample", "processOrder", "void",
                List.of(), List.of(), 5, 47, 42, 1, List.of(), List.of());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-01", smell, RefactoringAction.EXTRACT_METHOD, null, ms, null);

        RefactoringDiff diff = generator.generateDiff(candidate);

        assertNotNull(diff);
        assertEquals("RC-01", diff.getCandidateId());
        assertEquals(RefactoringAction.EXTRACT_METHOD, diff.getAction());
        assertTrue(diff.getBeforeCode().contains("processOrder"));
        assertTrue(diff.getAfterCode().contains("executePreparationPhase") || diff.getAfterCode().contains("execute"));
        assertTrue(diff.getUnifiedDiff().contains("--- a/"));
        assertTrue(diff.getUnifiedDiff().contains("+++ b/"));
    }

    @Test
    void testDataClumpsDiffGeneration() {
        CodeSmell smell = new CodeSmell(
                SmellType.DATA_CLUMPS, "DataClumpExample", "demo.DataClumpExample",
                "createCustomerProfile", "sample-projects/smell-test-project1/src/main/java/demo/DataClumpExample.java",
                6, 6, "3", "ExcessiveParameterList", "Too many parameters");
        MethodStructure ms = new MethodStructure(
                "demo.DataClumpExample", "createCustomerProfile", "void",
                List.of("String", "String", "String", "String", "String"),
                List.of("firstName", "lastName", "streetAddress", "city", "zipCode"),
                6, 9, 4, 1, List.of(), List.of());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-02", smell, RefactoringAction.INTRODUCE_PARAMETER_OBJECT, null, ms, null);

        RefactoringDiff diff = generator.generateDiff(candidate);

        assertNotNull(diff);
        assertEquals(RefactoringAction.INTRODUCE_PARAMETER_OBJECT, diff.getAction());
        assertTrue(diff.getAfterCode().contains("public record"));
        assertTrue(diff.getAfterCode().contains("createCustomerProfile("));
    }

    @Test
    void testFeatureEnvyDiffGeneration() {
        CodeSmell smell = new CodeSmell(
                SmellType.FEATURE_ENVY, "FeatureEnvyExample", "demo.FeatureEnvyExample",
                "processExternalData", "sample-projects/smell-test-project1/src/main/java/demo/FeatureEnvyExample.java",
                1, 22, "3", "CouplingBetweenObjects", "High coupling");
        ClassStructure cs = new ClassStructure(
                "demo.FeatureEnvyExample", "FeatureEnvyExample", "demo", "FeatureEnvyExample.java",
                false, null, List.of(), Map.of("a", "TargetServiceA"), List.of());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-03", smell, RefactoringAction.MOVE_METHOD, null, null, cs);

        RefactoringDiff diff = generator.generateDiff(candidate);

        assertNotNull(diff);
        assertEquals(RefactoringAction.MOVE_METHOD, diff.getAction());
        assertTrue(diff.getAfterCode().contains("processExternalData"));
    }

    @Test
    void testGodClassDiffGeneration() {
        CodeSmell smell = new CodeSmell(
                SmellType.GOD_CLASS, "GodClassExample", "demo.GodClassExample",
                null, "sample-projects/smell-test-project1/src/main/java/demo/GodClassExample.java",
                3, 69, "3", "TooManyMethods", "Too many methods");
        ClassStructure cs = new ClassStructure(
                "demo.GodClassExample", "GodClassExample", "demo", "GodClassExample.java",
                false, null, List.of(), Map.of(), List.of());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-04", smell, RefactoringAction.EXTRACT_CLASS, null, null, cs);

        RefactoringDiff diff = generator.generateDiff(candidate);

        assertNotNull(diff);
        assertEquals(RefactoringAction.EXTRACT_CLASS, diff.getAction());
        assertTrue(diff.getAfterCode().contains("class"));
    }
}
