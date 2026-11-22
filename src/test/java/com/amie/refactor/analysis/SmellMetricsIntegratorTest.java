package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassAnalysis;
import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.CombinedAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisStatus;
import com.amie.refactor.model.SmellType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SmellMetricsIntegrator}. No external tools are required: PMD
 * findings and CK metrics are constructed directly.
 */
class SmellMetricsIntegratorTest {

    private final SmellMetricsIntegrator integrator = new SmellMetricsIntegrator();

    private static CodeSmell smell(String fqn, SmellType type, String rule) {
        int dot = fqn.lastIndexOf('.');
        String simple = dot < 0 ? fqn : fqn.substring(dot + 1);
        String file = fqn.replace('.', '/') + ".java";
        return new CodeSmell(type, simple, fqn, "someMethod", file, 5, 20, "3", rule, "message");
    }

    private static ClassMetrics metrics(String fqn, int cbo, int wmc, int lcom) {
        return new ClassMetrics(fqn, fqn.replace('.', '/') + ".java", "class", cbo, wmc, lcom);
    }

    private static MetricsAnalysisResult metricsResult(List<ClassMetrics> list) {
        return new MetricsAnalysisResult("target", "out",
                MetricsAnalysisStatus.SUCCESS, list, List.of(), List.of());
    }

    private static ClassAnalysis find(CombinedAnalysisResult result, String fqn) {
        return result.getClasses().stream()
                .filter(c -> c.getFullyQualifiedClassName().equals(fqn))
                .findFirst()
                .orElseThrow(() -> new AssertionError("class not found: " + fqn));
    }

    @Test
    void combinesSmellAndMetricsForTheSameClass() {
        CodeSmell godSmell = smell("com.example.order.OrderService", SmellType.GOD_CLASS, "GodClass");
        MetricsAnalysisResult metrics = metricsResult(
                List.of(metrics("com.example.order.OrderService", 12, 40, 9)));

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(godSmell), metrics);

        assertEquals(1, result.getClasses().size());
        ClassAnalysis analysis = find(result, "com.example.order.OrderService");
        assertTrue(analysis.hasCodeSmells());
        assertTrue(analysis.hasMetrics());
        assertEquals(12, analysis.getMetrics().getCbo());
        assertEquals(40, analysis.getMetrics().getWmc());
        assertEquals(9, analysis.getMetrics().getLcom());
        assertEquals(MetricsAnalysisStatus.SUCCESS, result.getStatus());
    }

    @Test
    void keepsClassWithMetricsButNoSmells() {
        MetricsAnalysisResult metrics = metricsResult(List.of(metrics("demo.Clean", 0, 1, 0)));

        CombinedAnalysisResult result = integrator.combine(Path.of("proj"), List.of(), metrics);

        ClassAnalysis analysis = find(result, "demo.Clean");
        assertFalse(analysis.hasCodeSmells());
        assertTrue(analysis.hasMetrics());
        assertFalse(result.getWarnings().stream().anyMatch(w -> w.contains("No CK metrics")),
                "a class with metrics but no smells must not warn about missing metrics");
    }

    @Test
    void keepsClassWithSmellButNoMetricsAndReportsIt() {
        CodeSmell onlySmell = smell("demo.Smelly", SmellType.LONG_METHOD, "NcssCount");

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(onlySmell), metricsResult(List.of()));

        ClassAnalysis analysis = find(result, "demo.Smelly");
        assertTrue(analysis.hasCodeSmells());
        assertNull(analysis.getMetrics());
        assertEquals(MetricsAnalysisStatus.PARTIAL, result.getStatus());
        assertTrue(result.getWarnings().stream()
                        .anyMatch(w -> w.contains("demo.Smelly") && w.contains("No CK metrics")),
                "the unmatched smelly class must be reported");
    }

    @Test
    void distinguishesClassesWithSameSimpleNameInDifferentPackages() {
        CodeSmell inA = smell("a.Foo", SmellType.GOD_CLASS, "GodClass");
        CodeSmell inB = smell("b.Foo", SmellType.LONG_METHOD, "NcssCount");
        MetricsAnalysisResult metrics = metricsResult(List.of(
                metrics("a.Foo", 1, 10, 1),
                metrics("b.Foo", 2, 20, 2)));

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(inA, inB), metrics);

        assertEquals(2, result.getClasses().size());
        assertEquals(1, find(result, "a.Foo").getMetrics().getCbo());
        assertEquals(2, find(result, "b.Foo").getMetrics().getCbo());
        assertEquals(SmellType.GOD_CLASS, find(result, "a.Foo").getCodeSmells().get(0).getSmellType());
        assertEquals(SmellType.LONG_METHOD, find(result, "b.Foo").getCodeSmells().get(0).getSmellType());
    }

    @Test
    void doesNotAttachMetricsToTheWrongClass() {
        CodeSmell smellInA = smell("a.Foo", SmellType.GOD_CLASS, "GodClass");
        // Metrics exist only for an unrelated class with a different package.
        MetricsAnalysisResult metrics = metricsResult(List.of(metrics("b.Foo", 7, 7, 7)));

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(smellInA), metrics);

        // a.Foo has a smell but must NOT receive b.Foo's metrics.
        assertNull(find(result, "a.Foo").getMetrics());
        // b.Foo keeps its own metrics even though it has no smell.
        assertEquals(7, find(result, "b.Foo").getMetrics().getCbo());
    }

    @Test
    void reportsDuplicateMetricEntriesForTheSameClass() {
        MetricsAnalysisResult metrics = metricsResult(List.of(
                metrics("demo.Dup", 1, 1, 1),
                metrics("demo.Dup", 2, 2, 2)));

        CombinedAnalysisResult result = integrator.combine(Path.of("proj"), List.of(), metrics);

        assertEquals(1, result.getClasses().size());
        assertEquals(1, find(result, "demo.Dup").getMetrics().getCbo()); // first kept
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("More than one")));
    }

    @Test
    void matchesNestedClassesAcrossPMDAndCkNaming() {
        // CK reports the binary name (with '$'); PMD may report either form.
        CodeSmell smellInInner = smell("demo.Outer$Inner", SmellType.FEATURE_ENVY, "CouplingBetweenObjects");
        MetricsAnalysisResult metrics = metricsResult(List.of(metrics("demo.Outer$Inner", 5, 6, 1)));

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(smellInInner), metrics);

        ClassAnalysis analysis = find(result, "demo.Outer$Inner");
        assertTrue(analysis.hasCodeSmells());
        assertTrue(analysis.hasMetrics());
    }

    @Test
    void preservesPmdFindingsAndSourceLocations() {
        CodeSmell original = smell("demo.Keep", SmellType.DATA_CLUMPS, "ExcessiveParameterList");

        CombinedAnalysisResult result = integrator.combine(
                Path.of("proj"), List.of(original), metricsResult(List.of()));

        CodeSmell preserved = find(result, "demo.Keep").getCodeSmells().get(0);
        assertSame(original, preserved);
        assertEquals("demo/Keep.java", preserved.getFilePath());
        assertEquals(5, preserved.getStartLine());
        assertEquals(20, preserved.getEndLine());
        assertEquals("ExcessiveParameterList", preserved.getRuleName());
    }

    @Test
    void reportsFailureWhenNeitherSmellsNorMetricsAreAvailable() {
        CombinedAnalysisResult result = integrator.combine(Path.of("proj"), List.of(), null);

        assertTrue(result.getClasses().isEmpty());
        assertEquals(MetricsAnalysisStatus.FAILED, result.getStatus());
    }
}