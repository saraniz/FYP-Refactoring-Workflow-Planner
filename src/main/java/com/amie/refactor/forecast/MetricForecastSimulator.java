package com.amie.refactor.forecast;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.Dependency;
import com.amie.refactor.model.dependency.MethodStructure;
import com.amie.refactor.planning.RefactoringCandidate;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates "What-If" metric forecasts and calculates effort / blast radius indicators
 * grounded in AST structure, call graphs, and empirical Fowler refactoring patterns.
 */
public class MetricForecastSimulator {

    /**
     * Computes the simulated What-If metric forecast for a refactoring candidate.
     */
    public MetricForecast simulate(RefactoringCandidate candidate) {
        ClassMetrics cm = candidate.getClassMetrics();
        MethodStructure ms = candidate.getMethodStructure();

        int wmcBefore = cm != null ? cm.getWmc() : (ms != null ? ms.getCyclomaticComplexity() : 5);
        int lcomBefore = cm != null ? cm.getLcom() : 10;
        int cboBefore = cm != null ? cm.getCbo() : 1;
        int locBefore = (candidate.getEndLine() > candidate.getStartLine())
                ? (candidate.getEndLine() - candidate.getStartLine() + 1)
                : (ms != null ? ms.getLineCount() : 20);

        int paramBefore = ms != null ? ms.getParameterTypes().size() : 0;

        int wmcAfter = wmcBefore;
        int lcomAfter = lcomBefore;
        int cboAfter = cboBefore;
        int locAfter = locBefore;
        int paramAfter = paramBefore;
        String summary;

        switch (candidate.getSmellType()) {
            case GOD_CLASS -> {
                // Extract Class delegates roughly 50-60% of responsibilities to new cohesive class
                wmcAfter = Math.max(1, (int) Math.round(wmcBefore * 0.45));
                lcomAfter = Math.max(0, (int) Math.round(lcomBefore * 0.25));
                cboAfter = cboBefore + 1; // Delegating to newly extracted domain manager
                locAfter = Math.max(10, (int) Math.round(locBefore * 0.40));
                summary = String.format("Extract Class decouples domain responsibilities, cutting WMC from %d to %d (-%d%%) and eliminating low cohesion (LCOM %d to %d).",
                        wmcBefore, wmcAfter, (int) Math.round(((wmcBefore - wmcAfter) / (double) Math.max(1, wmcBefore)) * 100),
                        lcomBefore, lcomAfter);
            }
            case LONG_METHOD -> {
                // Extract Method decomposes monolith into cohesive subroutines
                int methodCC = ms != null ? ms.getCyclomaticComplexity() : 8;
                wmcAfter = Math.max(1, wmcBefore - Math.max(1, (int) Math.round(methodCC * 0.60)));
                locAfter = Math.max(5, (int) Math.round(locBefore * 0.35)); // orchestrator method length
                summary = String.format("Extract Method modularizes monolithic method into 2-3 focused subroutines, reducing orchestrator length from %d to %d lines (-%d%%).",
                        locBefore, locAfter, (int) Math.round(((locBefore - locAfter) / (double) Math.max(1, locBefore)) * 100));
            }
            case FEATURE_ENVY -> {
                // Move Method relocates method to the foreign data owner
                cboAfter = Math.max(0, cboBefore - 1);
                lcomAfter = Math.max(0, (int) Math.round(lcomBefore * 0.80));
                summary = String.format("Move Method relocates foreign data-access logic, reducing coupling (CBO: %d -> %d) and improving class cohesion.",
                        cboBefore, cboAfter);
            }
            case DATA_CLUMPS -> {
                // Introduce Parameter Object replaces repeated parameters with a single object
                paramAfter = 1;
                cboAfter = cboBefore + 1;
                summary = String.format("Introduce Parameter Object replaces %d primitive arguments with a unified parameter value object.",
                        paramBefore > 0 ? paramBefore : 4);
            }
            default -> summary = "Refactoring improves maintainability and stabilizes class structure.";
        }

        return new MetricForecast(
                wmcBefore, wmcAfter,
                lcomBefore, lcomAfter,
                cboBefore, cboAfter,
                locBefore, locAfter,
                paramBefore, paramAfter,
                summary
        );
    }

    /**
     * Evaluates the Effort and Blast Radius Indicator for a candidate based on Fan-In/Fan-Out and incoming callers.
     */
    public BlastRadiusInfo evaluateBlastRadius(RefactoringCandidate candidate) {
        ClassStructure cs = candidate.getClassStructure();
        int fanIn = cs != null ? cs.getFanIn() : 0;
        int fanOut = cs != null ? cs.getFanOut() : 0;

        List<String> affectedCallers = new ArrayList<>();
        if (cs != null) {
            for (Dependency dep : cs.getIncomingDependencies()) {
                String caller = dep.getSourceClass();
                if (!affectedCallers.contains(caller)) {
                    affectedCallers.add(caller);
                }
            }
        }

        RiskCategory category;
        String rationale;

        if (fanIn == 0 && fanOut <= 2) {
            category = RiskCategory.QUICK_WIN;
            rationale = "Quick Win: Zero incoming external dependencies. This refactoring can be applied immediately with zero ripple risk across other classes.";
        } else if (fanIn <= 2 && fanOut <= 5) {
            category = RiskCategory.LOW_RISK;
            rationale = String.format("Low Risk: Limited ripple potential (Fan-In: %d, Fan-Out: %d). %d caller(s) should be re-tested after refactoring.",
                    fanIn, fanOut, affectedCallers.size());
        } else if (fanIn <= 6) {
            category = RiskCategory.MODERATE_RIPPLE;
            rationale = String.format("Moderate Ripple Risk: %d incoming callers depend on this class (%s). Changes require signature preservation or regression testing.",
                    fanIn, String.join(", ", affectedCallers));
        } else {
            category = RiskCategory.HIGH_RIPPLE_RISK;
            rationale = String.format("High Ripple Risk: Core architectural class with %d callers (Fan-In: %d). Changes have a large blast radius and must follow backward-compatible deprecation.",
                    affectedCallers.size(), fanIn);
        }

        return new BlastRadiusInfo(fanIn, fanOut, category, affectedCallers, rationale);
    }
}
