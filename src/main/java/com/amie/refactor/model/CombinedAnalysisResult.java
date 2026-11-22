package com.amie.refactor.model;

import java.util.List;

/**
 * Result of combining PMD code-smell findings with CK Tool metrics into a single
 * per-class view.
 *
 * <p>This is the integration point the (future) refactoring planner consumes. Metrics
 * collection stays separate from prioritisation: this object only holds the observed
 * facts and their provenance, never thresholds or rankings.</p>
 */
public class CombinedAnalysisResult {

    private final String targetDirectory;
    private final MetricsAnalysisStatus status;
    private final List<ClassAnalysis> classes;
    private final List<String> warnings;
    private final List<String> errors;
    private final MetricsAnalysisResult metricsAnalysisResult;

    public CombinedAnalysisResult(
            String targetDirectory,
            MetricsAnalysisStatus status,
            List<ClassAnalysis> classes,
            List<String> warnings,
            List<String> errors,
            MetricsAnalysisResult metricsAnalysisResult) {

        this.targetDirectory = targetDirectory;
        this.status = status;
        this.classes = List.copyOf(classes);
        this.warnings = List.copyOf(warnings);
        this.errors = List.copyOf(errors);
        this.metricsAnalysisResult = metricsAnalysisResult;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public MetricsAnalysisStatus getStatus() {
        return status;
    }

    /** One entry per class known to PMD and/or CK Tool, ordered by first appearance. */
    public List<ClassAnalysis> getClasses() {
        return classes;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    /** The underlying metrics analysis result, or {@code null} if metrics were not run. */
    public MetricsAnalysisResult getMetricsAnalysisResult() {
        return metricsAnalysisResult;
    }

    @Override
    public String toString() {
        return "CombinedAnalysisResult{status=" + status
                + ", classes=" + classes.size()
                + ", warnings=" + warnings.size()
                + ", errors=" + errors.size()
                + '}';
    }
}