package com.amie.refactor.model;

import java.util.Objects;

/**
 * Class-level software metrics for a single Java type, as reported by the CK Tool.
 *
 * <p>Values are stored exactly as CK emitted them; nothing is recomputed or fabricated.
 * CK Tool 0.7.0 reports {@code cbo}, {@code wmc} and {@code lcom} as integers (LCOM is
 * clamped to {@code >= 0} by CK), therefore {@link Integer} is the natural numeric type.
 * A {@code null} value means the metric was <em>not present</em> in the CK output for that
 * class and must not be interpreted as zero.</p>
 *
 * <p>{@link #getClassName()} holds the CK "class" column verbatim. CK 0.7.0 writes the
 * binary name of the type (for example {@code demo.GodClassExample}, or
 * {@code demo.Outer$Inner} for a nested type), which for a top-level class is its fully
 * qualified name.</p>
 */
public class ClassMetrics {

    private final String className;
    private final String sourceFile;
    private final String type;
    private final Integer cbo;
    private final Integer wmc;
    private final Integer lcom;

    public ClassMetrics(
            String className,
            String sourceFile,
            String type,
            Integer cbo,
            Integer wmc,
            Integer lcom) {

        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Class name must not be blank");
        }

        this.className = className;
        this.sourceFile = sourceFile;
        this.type = type;
        this.cbo = cbo;
        this.wmc = wmc;
        this.lcom = lcom;
    }

    /** The CK "class" column verbatim (binary name of the type). */
    public String getClassName() {
        return className;
    }

    /** The CK "file" column, or {@code null} when it was not present. */
    public String getSourceFile() {
        return sourceFile;
    }

    /** The CK "type" column (class, interface, enum, ...), or {@code null}. */
    public String getType() {
        return type;
    }

    public Integer getCbo() {
        return cbo;
    }

    public Integer getWmc() {
        return wmc;
    }

    public Integer getLcom() {
        return lcom;
    }

    /**
     * @return true when CBO, WMC and LCOM were all present in the CK output.
     */
    public boolean hasAllMetrics() {
        return cbo != null && wmc != null && lcom != null;
    }

    /**
     * @return a short, human-readable list of the metric columns that were absent from
     *         the CK output for this class (or {@code "none"}).
     */
    public String describeMissingMetrics() {
        StringBuilder sink = new StringBuilder();
        appendMissing(sink, "cbo", cbo);
        appendMissing(sink, "wmc", wmc);
        appendMissing(sink, "lcom", lcom);
        return sink.isEmpty() ? "none" : sink.toString();
    }

    private static void appendMissing(StringBuilder sink, String name, Integer value) {
        if (value == null) {
            if (!sink.isEmpty()) {
                sink.append(", ");
            }
            sink.append(name);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassMetrics other)) {
            return false;
        }
        return Objects.equals(className, other.className)
                && Objects.equals(sourceFile, other.sourceFile)
                && Objects.equals(type, other.type)
                && Objects.equals(cbo, other.cbo)
                && Objects.equals(wmc, other.wmc)
                && Objects.equals(lcom, other.lcom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, sourceFile, type, cbo, wmc, lcom);
    }

    @Override
    public String toString() {
        return "ClassMetrics{class=" + className
                + ", cbo=" + cbo
                + ", wmc=" + wmc
                + ", lcom=" + lcom
                + '}';
    }
}