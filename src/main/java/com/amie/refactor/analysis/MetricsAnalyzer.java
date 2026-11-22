package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.MetricsAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the metrics analysis module (Part A).
 *
 * <p>Coordinates {@link CKToolRunner} (executes CK Tool) and {@link MetricsCsvParser}
 * (parses {@code class.csv}). This class owns the orchestration and the
 * success/partial/failure decision; it neither runs processes nor parses CSV itself.</p>
 *
 * <p>Failures are reported as a {@link MetricsAnalysisResult} with status
 * {@link MetricsAnalysisStatus#FAILED} and an explanatory error, never as an empty
 * "successful" result. When some results are usable the status is
 * {@link MetricsAnalysisStatus#PARTIAL}.</p>
 */
public class MetricsAnalyzer {

    /** Default directory CK CSV output is written to (project-relative). */
    public static final String DEFAULT_OUTPUT_DIRECTORY = "results/ck-metrics";

    private final CKToolRunner runner;
    private final MetricsCsvParser parser;

    /**
     * Creates an analyzer configured from the environment: the CK JAR is resolved from
     * {@code -Dck.jar} / {@code CK_JAR} / the default {@code tools/} location; CSV output
     * goes to {@link #DEFAULT_OUTPUT_DIRECTORY}.
     */
    public MetricsAnalyzer() {
        this(new CKToolRunner(
                        CKToolRunner.resolveCkJar(),
                        Path.of(DEFAULT_OUTPUT_DIRECTORY),
                        CKToolRunner.DEFAULT_TIMEOUT,
                        false),
                new MetricsCsvParser());
    }

    /** Creates an analyzer with explicit collaborators (tests / dependency injection). */
    public MetricsAnalyzer(CKToolRunner runner, MetricsCsvParser parser) {
        this.runner = runner;
        this.parser = parser;
    }

    /**
     * Analyses {@code sourceDirectory} and returns structured metrics. This method never
     * throws for expected failures; inspect {@link MetricsAnalysisResult#getStatus()}.
     */
    public MetricsAnalysisResult analyze(Path sourceDirectory) {

        String target = String.valueOf(sourceDirectory);
        String output = runner.getOutputDirectory() == null
                ? null
                : runner.getOutputDirectory().toString();

        if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
            return MetricsAnalysisResult.failed(target, output,
                    "Target source directory does not exist or is not a directory: "
                            + describe(sourceDirectory));
        }

        try {
            if (new ProjectScanner().findJavaFiles(sourceDirectory).isEmpty()) {
                return MetricsAnalysisResult.failed(target, output,
                        "No Java source files were found in: " + sourceDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            return MetricsAnalysisResult.failed(target, output,
                    "Unable to scan the target directory for Java files: " + e.getMessage());
        }

        Path classCsv;
        try {
            classCsv = runner.run(sourceDirectory);
        } catch (CKToolException e) {
            return MetricsAnalysisResult.failed(target, output, e.getMessage());
        } catch (IOException e) {
            return MetricsAnalysisResult.failed(target, output,
                    "CK Tool could not be started: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MetricsAnalysisResult.failed(target, output,
                    "CK Tool execution was interrupted");
        }

        List<ClassMetrics> metrics;
        try {
            metrics = parser.parse(classCsv);
        } catch (CKToolException e) {
            return MetricsAnalysisResult.failed(target, output,
                    "Failed to parse CK metrics output: " + e.getMessage());
        } catch (IOException e) {
            return MetricsAnalysisResult.failed(target, output,
                    "Failed to read CK metrics output: " + e.getMessage());
        }

        if (metrics.isEmpty()) {
            return MetricsAnalysisResult.failed(target, output,
                    "CK Tool produced no class records in " + classCsv.toAbsolutePath());
        }

        List<String> warnings = new ArrayList<>();
        int incomplete = 0;
        for (ClassMetrics metric : metrics) {
            if (!metric.hasAllMetrics()) {
                incomplete++;
                warnings.add("Missing CK metric value(s) for class " + metric.getClassName()
                        + ": " + metric.describeMissingMetrics());
            }
        }

        MetricsAnalysisStatus status = incomplete == 0
                ? MetricsAnalysisStatus.SUCCESS
                : MetricsAnalysisStatus.PARTIAL;
        if (incomplete > 0) {
            warnings.add(incomplete + " class(es) had incomplete CK metric values; the "
                    + "values that were present have been preserved.");
        }

        return new MetricsAnalysisResult(target, output, status, metrics, warnings, List.of());
    }

    private static String describe(Path path) {
        return path == null ? "null" : path.toAbsolutePath().toString();
    }
}