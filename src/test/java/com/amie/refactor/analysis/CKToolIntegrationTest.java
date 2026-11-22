package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassAnalysis;
import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.CombinedAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests that run the real CK Tool against the committed sample project.
 *
 * <p>They only execute when the CK JAR is configured, for example:</p>
 * <pre>
 *   mvn test -Dck.jar=tools/ck-0.7.0-jar-with-dependencies.jar
 * </pre>
 * <p>Without that property the whole class is skipped (never faked), so a normal
 * {@code mvn test} run stays green on machines without CK Tool.</p>
 */
@EnabledIfSystemProperty(named = "ck.jar", matches = ".+")
class CKToolIntegrationTest {

    private static final Path SAMPLE_SOURCES =
            Path.of("sample-projects", "smell-test-project1", "src", "main", "java");
    private static final Path SAMPLE_PMD_XML =
            Path.of("sample-projects", "smell-test-project1", "target", "pmd.xml");

    @TempDir
    Path outputDir;

    private MetricsAnalysisResult analyzeSampleProject() {
        CKToolRunner runner = new CKToolRunner(
                CKToolRunner.resolveCkJar(), outputDir, CKToolRunner.DEFAULT_TIMEOUT, false);
        return new MetricsAnalyzer(runner, new MetricsCsvParser()).analyze(SAMPLE_SOURCES);
    }

    private static ClassMetrics findMetrics(MetricsAnalysisResult result, String fqn) {
        return result.getClassMetrics().stream()
                .filter(m -> m.getClassName().equals(fqn))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no CK metrics for " + fqn
                        + "; got: " + result.getClassMetrics()));
    }

    @Test
    void ckToolProducesCboWmcAndLcomForTheSampleProject() {
        MetricsAnalysisResult result = analyzeSampleProject();

        assertFalse(result.isFailed(), "CK analysis failed: " + result.getErrors());
        assertTrue(result.getStatus() == MetricsAnalysisStatus.SUCCESS
                        || result.getStatus() == MetricsAnalysisStatus.PARTIAL,
                "unexpected status: " + result.getStatus());

        ClassMetrics godClass = findMetrics(result, "demo.GodClassExample");
        assertNotNull(godClass.getCbo());
        assertNotNull(godClass.getWmc());
        assertNotNull(godClass.getLcom());
        // Values come from CK Tool; sanity-check they are non-negative integers.
        assertTrue(godClass.getCbo() >= 0 && godClass.getWmc() >= 0 && godClass.getLcom() >= 0);

        // Sanity: the class with one short method should have a smaller WMC than the big one.
        ClassMetrics longMethod = findMetrics(result, "demo.LongMethodExample");
        assertTrue(godClass.getWmc() >= longMethod.getWmc());
    }

    @Test
    void combinesRealCkMetricsWithCommittedPmdFindings() throws Exception {
        assumeTrue(Files.isRegularFile(SAMPLE_PMD_XML),
                "committed PMD output not found; run 'mvn pmd:pmd' in the sample project first");

        List<CodeSmell> smells = new PmdResultParser().parse(SAMPLE_PMD_XML);
        assertFalse(smells.isEmpty(), "expected the PMD module to report findings");

        MetricsAnalysisResult metrics = analyzeSampleProject();
        CombinedAnalysisResult combined =
                new SmellMetricsIntegrator().combine(SAMPLE_SOURCES, smells, metrics);

        ClassAnalysis godClass = combined.getClasses().stream()
                .filter(c -> c.getFullyQualifiedClassName().equals("demo.GodClassExample"))
                .findFirst()
                .orElseThrow();

        assertTrue(godClass.hasCodeSmells(), "PMD finding should be preserved");
        assertTrue(godClass.hasMetrics(), "CK metrics should be attached to the same class");
        assertEquals(metrics.getClassMetrics().stream()
                        .filter(m -> m.getClassName().equals("demo.GodClassExample"))
                        .findFirst().orElseThrow().getCbo(),
                godClass.getMetrics().getCbo(), "the attached CBO must be the real CK value");
    }
}