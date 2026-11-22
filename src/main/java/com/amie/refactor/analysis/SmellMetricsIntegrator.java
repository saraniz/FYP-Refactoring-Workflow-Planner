package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassAnalysis;
import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.CombinedAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Smallest integration layer that associates PMD code-smell findings with CK Tool metrics
 * on a per-class basis.
 *
 * <p>Matching uses <strong>fully qualified class names</strong> (PMD's {@code package} +
 * {@code class}, CK's {@code class} column), never simple class names, so two classes that
 * share a simple name in different packages are never confused.</p>
 *
 * <p>Nothing is silently discarded:</p>
 * <ul>
 *   <li>a class with smells but no metrics is kept and reported with a warning;</li>
 *   <li>a class with metrics but no smells is kept with an empty smell list (the normal,
 *       clean case, and no warning);</li>
 *   <li>a class whose identifier cannot be resolved is reported rather than attached to
 *       the wrong class.</li>
 * </ul>
 *
 * <p>PMD findings and their source locations are never modified; the exact
 * {@link CodeSmell} instances are carried through into the combined result.</p>
 */
public class SmellMetricsIntegrator {

    /**
     * Combines the two analyses into one per-class view.
     *
     * @param projectDirectory target project (provenance only)
     * @param codeSmells       PMD findings (may be empty or {@code null})
     * @param metricsResult    CK metrics result (may be {@code null} if metrics were not run)
     */
    public CombinedAnalysisResult combine(
            Path projectDirectory,
            List<CodeSmell> codeSmells,
            MetricsAnalysisResult metricsResult) {

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Map<String, ClassAnalysisBuilder> classes = new LinkedHashMap<>();

        // 1. Group PMD findings by normalised fully qualified class name.
        if (codeSmells != null) {
            for (CodeSmell smell : codeSmells) {
                String identifier = identifierOf(smell);
                if (identifier == null || identifier.isBlank()) {
                    warnings.add("Skipped a PMD finding whose class identifier could not be "
                            + "determined (rule=" + smell.getRuleName()
                            + ", class=" + smell.getClassName() + ")");
                    continue;
                }
                classes.computeIfAbsent(normalise(identifier),
                                key -> new ClassAnalysisBuilder(identifier))
                        .addSmell(smell);
            }
        }

        // 2. Attach CK metrics to the matching classes.
        List<ClassMetrics> metrics = metricsResult == null
                ? List.of()
                : metricsResult.getClassMetrics();
        boolean metricsAvailable = metricsResult != null
                && metricsResult.getStatus() != MetricsAnalysisStatus.FAILED;

        for (ClassMetrics metric : metrics) {
            String key = normalise(metric.getClassName());
            ClassAnalysisBuilder builder = classes.get(key);
            if (builder == null) {
                // Class with metrics but no smells: the normal, clean case.
                classes.put(key, new ClassAnalysisBuilder(metric.getClassName()).setMetrics(metric));
            } else if (builder.metrics != null) {
                warnings.add("More than one CK metrics entry maps to class '"
                        + metric.getClassName() + "'; keeping the first entry and ignoring "
                        + "the duplicate.");
            } else {
                builder.setMetrics(metric);
            }
        }

        // 3. Report classes that PMD flagged but for which CK produced no metrics.
        if (metricsAvailable) {
            for (ClassAnalysisBuilder builder : classes.values()) {
                if (builder.hasSmells() && !builder.hasMetrics()) {
                    warnings.add("No CK metrics were found for class "
                            + builder.fullyQualifiedClassName
                            + " although PMD reported findings for it (the class may be absent "
                            + "from the CK output, for example a local or non-top-level class).");
                }
            }
        }

        // 4. Assemble the immutable combined result.
        List<ClassAnalysis> combined = new ArrayList<>(classes.size());
        for (ClassAnalysisBuilder builder : classes.values()) {
            combined.add(builder.build());
        }

        if (metricsResult != null) {
            warnings.addAll(metricsResult.getWarnings());
        }

        return new CombinedAnalysisResult(
                projectDirectory == null ? null : projectDirectory.toString(),
                combinedStatus(metricsResult, combined),
                combined,
                warnings,
                errors,
                metricsResult);
    }

    private MetricsAnalysisStatus combinedStatus(
            MetricsAnalysisResult metricsResult,
            List<ClassAnalysis> classes) {

        if (metricsResult == null
                || metricsResult.getStatus() == MetricsAnalysisStatus.FAILED) {
            // Metrics unavailable: this is only a failure when nothing at all is known.
            boolean anyData = !classes.isEmpty();
            return anyData ? MetricsAnalysisStatus.PARTIAL : MetricsAnalysisStatus.FAILED;
        }
        if (metricsResult.getStatus() == MetricsAnalysisStatus.PARTIAL) {
            return MetricsAnalysisStatus.PARTIAL;
        }

        boolean smellyClassWithoutMetrics = classes.stream()
                .anyMatch(c -> c.hasCodeSmells() && !c.hasMetrics());
        return smellyClassWithoutMetrics
                ? MetricsAnalysisStatus.PARTIAL
                : MetricsAnalysisStatus.SUCCESS;
    }

    /** Best available class identifier for a PMD finding, preferring the FQN. */
    private static String identifierOf(CodeSmell smell) {
        String fullyQualified = smell.getFullyQualifiedClassName();
        if (fullyQualified != null && !fullyQualified.isBlank()) {
            return fullyQualified;
        }
        return smell.getClassName();
    }

    /**
     * Normalises a class identifier so PMD and CK entries match. CK reports the binary name
     * ({@code demo.Outer$Inner} for nested types) while PMD may report either form, so
     * {@code '$'} is treated as a package separator. Top-level classes are unaffected.
     */
    private static String normalise(String className) {
        Objects.requireNonNull(className, "className");
        return className.trim().replace('$', '.');
    }

    /** Mutable per-class accumulator used while merging. */
    private static final class ClassAnalysisBuilder {

        private final String fullyQualifiedClassName;
        private final List<CodeSmell> smells = new ArrayList<>();
        private ClassMetrics metrics;

        ClassAnalysisBuilder(String fullyQualifiedClassName) {
            this.fullyQualifiedClassName = fullyQualifiedClassName;
        }

        ClassAnalysisBuilder addSmell(CodeSmell smell) {
            smells.add(smell);
            return this;
        }

        ClassAnalysisBuilder setMetrics(ClassMetrics metrics) {
            this.metrics = metrics;
            return this;
        }

        boolean hasSmells() {
            return !smells.isEmpty();
        }

        boolean hasMetrics() {
            return metrics != null;
        }

        ClassAnalysis build() {
            return new ClassAnalysis(fullyQualifiedClassName, smells, metrics);
        }
    }
}