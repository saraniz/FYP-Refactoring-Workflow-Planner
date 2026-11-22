package com.amie.refactor.planning;

/**
 * Quality improvement goals selectable by developers.
 * Defined in Objective 3 of the thesis proposal.
 */
public enum QualityGoal {

    /**
     * Focuses on structural quality, cohesion (LCOM), coupling (CBO),
     * and reducing architectural ripple effects.
     */
    IMPROVE_MAINTAINABILITY("Improve Maintainability",
            "Prioritizes structural cohesion, coupling reduction, and dependency stabilization."),

    /**
     * Focuses on reducing method-level and class-level complexity (WMC, NCSS line count, cyclomatic complexity).
     */
    REDUCE_COMPLEXITY("Reduce Complexity",
            "Prioritizes high-complexity methods, large control structures, and complex classes."),

    /**
     * Balances severity, metrics, dependencies, and expected improvement evenly.
     */
    BALANCED_IMPROVEMENT("Balanced Improvement",
            "Applies a balanced trade-off across smell severity, metrics, dependencies, and benefits.");

    private final String displayName;
    private final String description;

    QualityGoal(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
