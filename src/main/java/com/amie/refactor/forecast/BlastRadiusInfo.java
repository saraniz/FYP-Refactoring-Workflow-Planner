package com.amie.refactor.forecast;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the effort and blast radius analysis for a refactoring candidate,
 * quantifying caller ripple risks based on AST Fan-In / Fan-Out.
 */
public class BlastRadiusInfo {

    private final int fanIn;
    private final int fanOut;
    private final RiskCategory riskCategory;
    private final List<String> affectedCallers;
    private final String riskRationale;

    public BlastRadiusInfo(
            int fanIn,
            int fanOut,
            RiskCategory riskCategory,
            List<String> affectedCallers,
            String riskRationale) {

        this.fanIn = Math.max(0, fanIn);
        this.fanOut = Math.max(0, fanOut);
        this.riskCategory = Objects.requireNonNull(riskCategory, "riskCategory must not be null");
        this.affectedCallers = affectedCallers != null ? List.copyOf(affectedCallers) : List.of();
        this.riskRationale = riskRationale != null ? riskRationale : riskCategory.getDescription();
    }

    public int getFanIn() {
        return fanIn;
    }

    public int getFanOut() {
        return fanOut;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public String getRiskCategoryName() {
        return riskCategory.name();
    }

    public String getRiskBadge() {
        return riskCategory.getBadgeText();
    }

    public String getColorHex() {
        return riskCategory.getColorHex();
    }

    public List<String> getAffectedCallers() {
        return affectedCallers;
    }

    public String getRiskRationale() {
        return riskRationale;
    }

    @Override
    public String toString() {
        return riskCategory.getBadgeText() + " (Fan-In: " + fanIn + ", Fan-Out: " + fanOut
                + ", Callers Affected: " + affectedCallers.size() + ")";
    }
}
