package com.amie.refactor.planning;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.MethodStructure;

/**
 * Deterministic scoring engine for refactoring candidates.
 * Implements the multi-criteria formulation:
 * P_i = w_s * S_i + w_m * M_i + w_d * D_i + w_b * B_i
 */
public class PriorityScorer {

    private final ScoringConfiguration configuration;

    public PriorityScorer() {
        this(new ScoringConfiguration());
    }

    public PriorityScorer(ScoringConfiguration configuration) {
        this.configuration = configuration != null ? configuration : new ScoringConfiguration();
    }

    public ScoreBreakdown calculateScore(
            RefactoringCandidate candidate,
            QualityGoal goal,
            GoalWeights weights) {

        // 1. Smell Severity (S_i)
        double s = computeSeverity(candidate);
        String sRationale = explainSeverity(candidate, s);

        // 2. Metric Impact (M_i)
        double m = computeMetricImpact(candidate, goal);
        String mRationale = explainMetricImpact(candidate, goal, m);

        // 3. Dependency Impact (D_i)
        double d = computeDependencyImpact(candidate);
        String dRationale = explainDependencyImpact(candidate, d);

        // 4. Estimated Benefit (B_i)
        double b = computeEstimatedBenefit(candidate, goal);
        String bRationale = explainEstimatedBenefit(candidate, goal, b);

        // Final Priority Score (P_i)
        double p = weights.getSeverityWeight() * s
                + weights.getMetricWeight() * m
                + weights.getDependencyWeight() * d
                + weights.getBenefitWeight() * b;

        // Clamp to [0.0, 1.0]
        p = Math.max(0.0, Math.min(1.0, p));

        return new ScoreBreakdown(s, m, d, b, weights, p, sRationale, mRationale, dRationale, bRationale);
    }

    private double computeSeverity(RefactoringCandidate candidate) {
        String severityStr = candidate.getOriginalSmell().getSeverity();
        double baseSeverity = 0.65; // Default medium

        if (severityStr != null) {
            try {
                int pmdPriority = Integer.parseInt(severityStr.trim());
                baseSeverity = switch (pmdPriority) {
                    case 1 -> 1.00; // High
                    case 2 -> 0.85; // Medium-High
                    case 3 -> 0.65; // Medium
                    case 4 -> 0.45; // Medium-Low
                    case 5 -> 0.25; // Low
                    default -> 0.50;
                };
            } catch (NumberFormatException ignored) {
                if (severityStr.equalsIgnoreCase("High")) baseSeverity = 0.90;
                else if (severityStr.equalsIgnoreCase("Medium")) baseSeverity = 0.60;
                else if (severityStr.equalsIgnoreCase("Low")) baseSeverity = 0.30;
            }
        }

        // Adjust for extreme dimensions
        MethodStructure ms = candidate.getMethodStructure();
        if (ms != null && ms.getLineCount() > configuration.getMethodLocSaturationThreshold()) {
            baseSeverity = Math.min(1.0, baseSeverity + 0.10);
        }

        ClassMetrics cm = candidate.getClassMetrics();
        if (cm != null && cm.getWmc() != null && cm.getWmc() > configuration.getWmcSaturationThreshold()) {
            baseSeverity = Math.min(1.0, baseSeverity + 0.10);
        }

        return Math.max(0.0, Math.min(1.0, baseSeverity));
    }

    private double computeMetricImpact(RefactoringCandidate candidate, QualityGoal goal) {
        ClassMetrics cm = candidate.getClassMetrics();
        double cboNorm = 0.0;
        double wmcNorm = 0.0;
        double lcomNorm = 0.0;

        if (cm != null) {
            if (cm.getCbo() != null) {
                cboNorm = Math.min(1.0, cm.getCbo() / configuration.getCboSaturationThreshold());
            }
            if (cm.getWmc() != null) {
                wmcNorm = Math.min(1.0, cm.getWmc() / configuration.getWmcSaturationThreshold());
            }
            if (cm.getLcom() != null) {
                lcomNorm = Math.min(1.0, cm.getLcom() / configuration.getLcomSaturationThreshold());
            }
        }

        MethodStructure ms = candidate.getMethodStructure();
        double ccNorm = 0.0;
        if (ms != null) {
            ccNorm = Math.min(1.0, ms.getCyclomaticComplexity() / configuration.getCyclomaticComplexitySaturationThreshold());
        }

        return switch (goal) {
            case IMPROVE_MAINTAINABILITY ->
                // Maintainability emphasizes coupling and cohesion
                    (0.45 * cboNorm) + (0.40 * lcomNorm) + (0.15 * wmcNorm);

            case REDUCE_COMPLEXITY ->
                // Complexity emphasizes method CC and class WMC
                    (0.60 * wmcNorm) + (0.30 * (ms != null ? ccNorm : wmcNorm)) + (0.10 * cboNorm);

            case BALANCED_IMPROVEMENT ->
                // Balanced trade-off
                    (0.40 * wmcNorm) + (0.30 * cboNorm) + (0.30 * lcomNorm);
        };
    }

    private double computeDependencyImpact(RefactoringCandidate candidate) {
        ClassStructure cs = candidate.getClassStructure();
        if (cs == null) {
            return 0.30; // Moderate default when structural details unavailable
        }

        int fanIn = cs.getFanIn();
        int fanOut = cs.getFanOut();

        // Ripple effect model: fan-in has higher impact because breaking changes propagate downstream
        double combinedWeight = (2.0 * fanIn) + (1.0 * fanOut);
        return Math.min(1.0, combinedWeight / configuration.getDependencySaturationThreshold());
    }

    private double computeEstimatedBenefit(RefactoringCandidate candidate, QualityGoal goal) {
        SmellType smell = candidate.getSmellType();
        double baseBenefit = switch (smell) {
            case GOD_CLASS -> 0.90;
            case LONG_METHOD -> 0.85;
            case FEATURE_ENVY -> 0.75;
            case DATA_CLUMPS -> 0.65;
        };

        // Goal alignment modulation
        if (goal == QualityGoal.REDUCE_COMPLEXITY && (smell == SmellType.LONG_METHOD || smell == SmellType.GOD_CLASS)) {
            baseBenefit = Math.min(1.0, baseBenefit + 0.08);
        } else if (goal == QualityGoal.IMPROVE_MAINTAINABILITY && (smell == SmellType.GOD_CLASS || smell == SmellType.FEATURE_ENVY)) {
            baseBenefit = Math.min(1.0, baseBenefit + 0.08);
        }

        return Math.min(1.0, baseBenefit);
    }

    private String explainSeverity(RefactoringCandidate c, double s) {
        return String.format("Severity score %.2f based on %s violation in %s (PMD severity: %s).",
                s, c.getSmellType(), c.getClassName(), c.getOriginalSmell().getSeverity());
    }

    private String explainMetricImpact(RefactoringCandidate c, QualityGoal goal, double m) {
        ClassMetrics cm = c.getClassMetrics();
        int cbo = cm != null && cm.getCbo() != null ? cm.getCbo() : 0;
        int wmc = cm != null && cm.getWmc() != null ? cm.getWmc() : 0;
        int lcom = cm != null && cm.getLcom() != null ? cm.getLcom() : 0;
        return String.format("Metric impact %.2f under '%s' goal (CBO=%d, WMC=%d, LCOM=%d).",
                m, goal.getDisplayName(), cbo, wmc, lcom);
    }

    private String explainDependencyImpact(RefactoringCandidate c, double d) {
        ClassStructure cs = c.getClassStructure();
        int fanIn = cs != null ? cs.getFanIn() : 0;
        int fanOut = cs != null ? cs.getFanOut() : 0;
        return String.format("Dependency impact %.2f reflecting ripple potential (Fan-in=%d callers, Fan-out=%d callees).",
                d, fanIn, fanOut);
    }

    private String explainEstimatedBenefit(RefactoringCandidate c, QualityGoal goal, double b) {
        return String.format("Estimated benefit %.2f applying %s to resolve %s for %s.",
                b, c.getRecommendedAction().getActionName(), c.getSmellType(), goal.getDisplayName());
    }
}
