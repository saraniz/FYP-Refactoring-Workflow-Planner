package com.amie.refactor.planning;

import java.util.Objects;

/**
 * Weights assigned to the four prioritization factors in the scoring function:
 * P_i = w_s * S_i + w_m * M_i + w_d * D_i + w_b * B_i
 *
 * <p>Where:
 * <ul>
 *   <li>w_s = smell severity weight</li>
 *   <li>w_m = metric impact weight</li>
 *   <li>w_d = dependency impact weight</li>
 *   <li>w_b = estimated improvement benefit weight</li>
 * </ul>
 * </p>
 */
public class GoalWeights {

    private final double severityWeight;
    private final double metricWeight;
    private final double dependencyWeight;
    private final double benefitWeight;

    public GoalWeights(double severityWeight, double metricWeight, double dependencyWeight, double benefitWeight) {
        if (severityWeight < 0 || metricWeight < 0 || dependencyWeight < 0 || benefitWeight < 0) {
            throw new IllegalArgumentException("Weights must be non-negative");
        }
        double sum = severityWeight + metricWeight + dependencyWeight + benefitWeight;
        if (Math.abs(sum - 1.0) > 0.001) {
            // Normalize to sum to 1.0
            this.severityWeight = severityWeight / sum;
            this.metricWeight = metricWeight / sum;
            this.dependencyWeight = dependencyWeight / sum;
            this.benefitWeight = benefitWeight / sum;
        } else {
            this.severityWeight = severityWeight;
            this.metricWeight = metricWeight;
            this.dependencyWeight = dependencyWeight;
            this.benefitWeight = benefitWeight;
        }
    }

    /**
     * Default goal-specific weight presets grounded in software engineering literature:
     * - Maintainability emphasizes coupling/cohesion metrics and dependency ripple effects.
     * - Complexity emphasizes method/class complexity and smell severity.
     * - Balanced evenly balances all 4 dimensions.
     */
    public static GoalWeights forGoal(QualityGoal goal) {
        return switch (goal) {
            case IMPROVE_MAINTAINABILITY -> new GoalWeights(0.20, 0.35, 0.25, 0.20);
            case REDUCE_COMPLEXITY -> new GoalWeights(0.30, 0.35, 0.15, 0.20);
            case BALANCED_IMPROVEMENT -> new GoalWeights(0.25, 0.25, 0.25, 0.25);
        };
    }

    public double getSeverityWeight() {
        return severityWeight;
    }

    public double getMetricWeight() {
        return metricWeight;
    }

    public double getDependencyWeight() {
        return dependencyWeight;
    }

    public double getBenefitWeight() {
        return benefitWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoalWeights that)) return false;
        return Double.compare(that.severityWeight, severityWeight) == 0 &&
                Double.compare(that.metricWeight, metricWeight) == 0 &&
                Double.compare(that.dependencyWeight, dependencyWeight) == 0 &&
                Double.compare(that.benefitWeight, benefitWeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(severityWeight, metricWeight, dependencyWeight, benefitWeight);
    }

    @Override
    public String toString() {
        return String.format("GoalWeights[w_s=%.2f, w_m=%.2f, w_d=%.2f, w_b=%.2f]",
                severityWeight, metricWeight, dependencyWeight, benefitWeight);
    }
}
