package com.amie.refactor.model;

import java.util.List;

/**
 * Combined, per-class view of the analysis: the code smells PMD detected for a class
 * together with the CK Tool metrics measured for the same class.
 *
 * <p>An instance is produced for every class known to <em>either</em> tool, so that
 * nothing is silently dropped:</p>
 * <ul>
 *   <li>{@link #getMetrics()} is {@code null} when CK Tool reported no metrics for the
 *       class (for example a class with PMD findings that CK did not emit);</li>
 *   <li>{@link #getCodeSmells()} is empty for a class that has metrics but no smells
 *       (the normal, clean case).</li>
 * </ul>
 */
public class ClassAnalysis {

    private final String fullyQualifiedClassName;
    private final List<CodeSmell> codeSmells;
    private final ClassMetrics metrics;

    public ClassAnalysis(
            String fullyQualifiedClassName,
            List<CodeSmell> codeSmells,
            ClassMetrics metrics) {

        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.codeSmells = List.copyOf(codeSmells);
        this.metrics = metrics;
    }

    /** Fully qualified class name used as the stable, cross-tool identifier. */
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public List<CodeSmell> getCodeSmells() {
        return codeSmells;
    }

    /** CK Tool metrics for this class, or {@code null} when none were reported. */
    public ClassMetrics getMetrics() {
        return metrics;
    }

    public boolean hasCodeSmells() {
        return !codeSmells.isEmpty();
    }

    public boolean hasMetrics() {
        return metrics != null;
    }

    @Override
    public String toString() {
        return "ClassAnalysis{class=" + fullyQualifiedClassName
                + ", smells=" + codeSmells.size()
                + ", hasMetrics=" + hasMetrics()
                + '}';
    }
}