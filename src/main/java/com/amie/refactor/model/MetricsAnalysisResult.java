package com.amie.refactor.model;

import java.util.List;

/**
 * Structured result of running the CK Tool metrics analysis for one target project.
 *
 * <p>Carries enough information to explain what happened: the analysed target, the
 * directory the CK CSV files were written to, the parsed per-class metrics, and any
 * warnings or errors. Failures are never hidden behind an empty "successful" result.</p>
 */
public class MetricsAnalysisResult {

    private final String targetDirectory;
    private final String outputDirectory;
    private final MetricsAnalysisStatus status;
    private final List<ClassMetrics> classMetrics;
    private final List<String> warnings;
    private final List<String> errors;

    public MetricsAnalysisResult(
            String targetDirectory,
            String outputDirectory,
            MetricsAnalysisStatus status,
            List<ClassMetrics> classMetrics,
            List<String> warnings,
            List<String> errors) {

        this.targetDirectory = targetDirectory;
        this.outputDirectory = outputDirectory;
        this.status = status;
        this.classMetrics = List.copyOf(classMetrics);
        this.warnings = List.copyOf(warnings);
        this.errors = List.copyOf(errors);
    }

    /**
     * Creates a failed result carrying a single explanatory error message.
     */
    public static MetricsAnalysisResult failed(
            String targetDirectory,
            String outputDirectory,
            String error) {

        return new MetricsAnalysisResult(
                targetDirectory,
                outputDirectory,
                MetricsAnalysisStatus.FAILED,
                List.of(),
                List.of(),
                List.of(error == null ? "Unknown CK Tool metrics failure" : error)
        );
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public MetricsAnalysisStatus getStatus() {
        return status;
    }

    public List<ClassMetrics> getClassMetrics() {
        return classMetrics;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isFailed() {
        return status == MetricsAnalysisStatus.FAILED;
    }

    public boolean isSuccess() {
        return status == MetricsAnalysisStatus.SUCCESS;
    }

    @Override
    public String toString() {
        return "MetricsAnalysisResult{status=" + status
                + ", classes=" + classMetrics.size()
                + ", warnings=" + warnings.size()
                + ", errors=" + errors.size()
                + '}';
    }
}