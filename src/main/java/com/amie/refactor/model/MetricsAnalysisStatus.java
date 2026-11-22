package com.amie.refactor.model;

/**
 * Outcome of a metrics analysis run.
 *
 * <p>{@link #SUCCESS} and {@link #PARTIAL} are deliberately distinct so that a partially
 * successful run (for example the tool ran but some metric values were missing) is never
 * reported as a complete success.</p>
 */
public enum MetricsAnalysisStatus {

    /** CK Tool ran and every produced class record carried all required metrics. */
    SUCCESS,

    /** CK Tool ran, but some results were missing, unparsable or unmatched. */
    PARTIAL,

    /** No usable metrics could be produced; see the associated errors. */
    FAILED
}