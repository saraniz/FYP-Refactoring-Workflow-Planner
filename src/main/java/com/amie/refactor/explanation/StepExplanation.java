package com.amie.refactor.explanation;

import java.util.Collections;
import java.util.List;

/**
 * Project-specific, grounded explanation for a single refactoring recommendation.
 * Follows the What-Where-Why-Ordering structure identified in the thesis literature review
 * (W3B / TORTOISE / DSRM frameworks).
 */
public class StepExplanation {

    private final String whatAndWhere;
    private final String whyRecommended;
    private final String whyPrioritized;
    private final String whyInThisSequence;
    private final List<String> actionableGuidance;

    public StepExplanation(
            String whatAndWhere,
            String whyRecommended,
            String whyPrioritized,
            String whyInThisSequence,
            List<String> actionableGuidance) {

        this.whatAndWhere = whatAndWhere;
        this.whyRecommended = whyRecommended;
        this.whyPrioritized = whyPrioritized;
        this.whyInThisSequence = whyInThisSequence;
        this.actionableGuidance = List.copyOf(actionableGuidance);
    }

    public String getWhatAndWhere() {
        return whatAndWhere;
    }

    public String getWhyRecommended() {
        return whyRecommended;
    }

    public String getWhyPrioritized() {
        return whyPrioritized;
    }

    public String getWhyInThisSequence() {
        return whyInThisSequence;
    }

    public List<String> getActionableGuidance() {
        return actionableGuidance;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("### What & Where\n").append(whatAndWhere).append("\n\n");
        sb.append("### Why Recommended\n").append(whyRecommended).append("\n\n");
        sb.append("### Why Prioritized\n").append(whyPrioritized).append("\n\n");
        sb.append("### Why In This Sequence\n").append(whyInThisSequence).append("\n\n");
        sb.append("### Actionable Steps\n");
        for (int i = 0; i < actionableGuidance.size(); i++) {
            sb.append((i + 1)).append(". ").append(actionableGuidance.get(i)).append("\n");
        }
        return sb.toString();
    }
}
