package com.amie.refactor.planning;

/**
 * Detailed breakdown of each term in the priority scoring calculation:
 * P_i = w_s * S_i + w_m * M_i + w_d * D_i + w_b * B_i
 *
 * <p>Enables fully explainable, traceable recommendations without black-box opacity.</p>
 */
public class ScoreBreakdown {

    private final double severityScore;
    private final double metricImpactScore;
    private final double dependencyImpactScore;
    private final double estimatedBenefitScore;

    private final double severityWeight;
    private final double metricWeight;
    private final double dependencyWeight;
    private final double benefitWeight;

    private final double finalPriorityScore;

    private final String severityRationale;
    private final String metricRationale;
    private final String dependencyRationale;
    private final String benefitRationale;

    public ScoreBreakdown(
            double severityScore,
            double metricImpactScore,
            double dependencyImpactScore,
            double estimatedBenefitScore,
            GoalWeights weights,
            double finalPriorityScore,
            String severityRationale,
            String metricRationale,
            String dependencyRationale,
            String benefitRationale) {

        this.severityScore = severityScore;
        this.metricImpactScore = metricImpactScore;
        this.dependencyImpactScore = dependencyImpactScore;
        this.estimatedBenefitScore = estimatedBenefitScore;

        this.severityWeight = weights.getSeverityWeight();
        this.metricWeight = weights.getMetricWeight();
        this.dependencyWeight = weights.getDependencyWeight();
        this.benefitWeight = weights.getBenefitWeight();

        this.finalPriorityScore = finalPriorityScore;

        this.severityRationale = severityRationale;
        this.metricRationale = metricRationale;
        this.dependencyRationale = dependencyRationale;
        this.benefitRationale = benefitRationale;
    }

    public double getSeverityScore() {
        return severityScore;
    }

    public double getMetricImpactScore() {
        return metricImpactScore;
    }

    public double getDependencyImpactScore() {
        return dependencyImpactScore;
    }

    public double getEstimatedBenefitScore() {
        return estimatedBenefitScore;
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

    public double getFinalPriorityScore() {
        return finalPriorityScore;
    }

    public String getSeverityRationale() {
        return severityRationale;
    }

    public String getMetricRationale() {
        return metricRationale;
    }

    public String getDependencyRationale() {
        return dependencyRationale;
    }

    public String getBenefitRationale() {
        return benefitRationale;
    }

    @Override
    public String toString() {
        return String.format(
                "Priority=%.3f (Severity=%.2f*%.2f + Metric=%.2f*%.2f + Dep=%.2f*%.2f + Benefit=%.2f*%.2f)",
                finalPriorityScore, severityScore, severityWeight,
                metricImpactScore, metricWeight,
                dependencyImpactScore, dependencyWeight,
                estimatedBenefitScore, benefitWeight);
    }
}
