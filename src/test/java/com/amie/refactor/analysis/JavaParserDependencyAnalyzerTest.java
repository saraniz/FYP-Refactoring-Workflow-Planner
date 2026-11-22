package com.amie.refactor.analysis;

import com.amie.refactor.analysis.dependency.JavaParserDependencyAnalyzer;
import com.amie.refactor.model.dependency.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JavaParserDependencyAnalyzerTest {

    private final Path sampleSourceDir =
            Path.of("sample-projects/smell-test-project1/src/main/java");

    @Test
    void testAnalyzeSampleProject() {
        JavaParserDependencyAnalyzer analyzer = new JavaParserDependencyAnalyzer();
        DependencyAnalysisResult result = analyzer.analyze(sampleSourceDir);

        assertNotNull(result);
        assertFalse(result.getClasses().isEmpty(), "Should extract classes from sample project");

        // Verify GodClassExample
        ClassStructure godClass = result.getClassStructure("demo.GodClassExample");
        assertNotNull(godClass, "GodClassExample should be found");
        assertEquals("GodClassExample", godClass.getSimpleName());
        assertTrue(godClass.getMethods().size() >= 10, "GodClassExample should have many methods");

        // Verify LongMethodExample
        ClassStructure longMethodClass = result.getClassStructure("demo.LongMethodExample");
        assertNotNull(longMethodClass, "LongMethodExample should be found");
        MethodStructure processOrder = longMethodClass.findMethod("processOrder").orElse(null);
        assertNotNull(processOrder, "processOrder method should be found");
        assertTrue(processOrder.getLineCount() >= 20, "processOrder should have line count >= 20");

        // Verify FeatureEnvyExample dependencies to TargetServices
        ClassStructure featureEnvyClass = result.getClassStructure("demo.FeatureEnvyExample");
        assertNotNull(featureEnvyClass, "FeatureEnvyExample should be found");
        assertTrue(featureEnvyClass.getFanOut() >= 4,
                "FeatureEnvyExample should depend on TargetServiceA, B, C, D (fan-out >= 4)");

        // Verify TargetServiceA has fanIn >= 1 (called by FeatureEnvyExample)
        ClassStructure targetA = result.getClassStructure("demo.TargetServiceA");
        assertNotNull(targetA, "TargetServiceA should be found");
        assertTrue(targetA.getFanIn() >= 1, "TargetServiceA should have incoming dependency from FeatureEnvyExample");

        // Verify direct dependency check
        assertTrue(result.hasDirectDependency("demo.FeatureEnvyExample", "demo.TargetServiceA"));
        assertFalse(result.hasDirectDependency("demo.TargetServiceA", "demo.LongMethodExample"));
    }

    @Test
    void testNonExistentDirectoryReturnsEmptyResultWithWarning() {
        JavaParserDependencyAnalyzer analyzer = new JavaParserDependencyAnalyzer();
        DependencyAnalysisResult result = analyzer.analyze(Path.of("non/existent/path"));

        assertNotNull(result);
        assertTrue(result.getClasses().isEmpty());
        assertFalse(result.getWarnings().isEmpty());
    }
}
