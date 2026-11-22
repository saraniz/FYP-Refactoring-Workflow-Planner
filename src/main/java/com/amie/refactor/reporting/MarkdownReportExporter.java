package com.amie.refactor.reporting;

import com.amie.refactor.explanation.WorkflowStep;
import com.amie.refactor.planning.RefactoringCandidate;
import com.amie.refactor.planning.ScoreBreakdown;
import com.amie.refactor.planning.WorkflowPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates formatted Markdown reports documenting the generated refactoring workflow,
 * comparative goal rankings, and code-aware step justifications.
 */
public class MarkdownReportExporter {

    public Path export(
            WorkflowPlan plan,
            List<WorkflowStep> steps,
            Map<String, List<RefactoringCandidate>> comparativeGoalRankings,
            Path outputFile) throws IOException {

        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");

        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }

        StringBuilder sb = new StringBuilder();

        sb.append("# Code-Aware Refactoring Workflow Plan\n\n");
        sb.append("**Target Project:** `").append(plan.getTargetDirectory()).append("`  \n");
        sb.append("**Selected Quality Goal:** **").append(plan.getQualityGoal().getDisplayName()).append("**  \n");
        sb.append("**Goal Description:** ").append(plan.getQualityGoal().getDescription()).append("  \n");
        sb.append("**Scoring Weights:** `").append(plan.getGoalWeights().toString()).append("`  \n");
        sb.append("**Planned Candidates:** ").append(plan.getCandidateCount()).append(" | ");
        sb.append("**Dependency Precedence Constraints:** ").append(plan.getDependencyConstraintCount()).append("  \n");
        sb.append("**Generated At:** ").append(plan.getPlannedAt()).append("\n\n");

        sb.append("---\n\n");
        sb.append("## 1. Prioritized Workflow Overview\n\n");
        sb.append("| Step | Candidate ID | Recommended Action | Target Element | Smell Type | Priority Score | Prerequisites |\n");
        sb.append("|:---:|:---:|:---|:---|:---|:---:|:---|\n");

        for (WorkflowStep step : steps) {
            RefactoringCandidate c = step.getCandidate();
            String target = c.getClassName() + (c.getMethodName() != null && !c.getMethodName().equalsIgnoreCase("N/A")
                    ? "." + c.getMethodName() + "()"
                    : "");
            String prereqs = c.getPrerequisiteIds().isEmpty() ? "None" : String.join(", ", c.getPrerequisiteIds());

            sb.append(String.format("| %d | `%s` | **%s** | `%s` | %s | **%.3f** | %s |\n",
                    step.getStepNumber(),
                    c.getCandidateId(),
                    c.getRecommendedAction().getActionName(),
                    target,
                    c.getSmellType(),
                    c.getPriorityScore(),
                    prereqs));
        }

        sb.append("\n---\n\n");

        // Section 2: Comparative Goal Prioritization (demonstrates research contribution)
        if (comparativeGoalRankings != null && !comparativeGoalRankings.isEmpty()) {
            sb.append("## 2. Comparative Analysis Across Quality Goals\n\n");
            sb.append("This section compares how priority scores and sequence order adapt when the developer switches quality improvement goals:\n\n");

            sb.append("| Candidate ID | Target Element | Smell Type | Maintainability Score | Complexity Score | Balanced Score |\n");
            sb.append("|:---:|:---|:---|:---:|:---:|:---:|\n");

            List<RefactoringCandidate> maintList = comparativeGoalRankings.get("IMPROVE_MAINTAINABILITY");
            List<RefactoringCandidate> compList = comparativeGoalRankings.get("REDUCE_COMPLEXITY");
            List<RefactoringCandidate> balList = comparativeGoalRankings.get("BALANCED_IMPROVEMENT");

            for (WorkflowStep step : steps) {
                String cid = step.getCandidate().getCandidateId();
                RefactoringCandidate c = step.getCandidate();
                String target = c.getClassName() + (c.getMethodName() != null && !c.getMethodName().equalsIgnoreCase("N/A")
                        ? "." + c.getMethodName() + "()"
                        : "");

                double maintScore = findScore(maintList, cid);
                double compScore = findScore(compList, cid);
                double balScore = findScore(balList, cid);

                sb.append(String.format("| `%s` | `%s` | %s | %.3f | %.3f | %.3f |\n",
                        cid, target, c.getSmellType(), maintScore, compScore, balScore));
            }

            sb.append("\n---\n\n");
        }

        // Section 3: Detailed Step Guidance
        sb.append("## 3. Step-by-Step Code-Aware Guidance\n\n");

        for (WorkflowStep step : steps) {
            RefactoringCandidate c = step.getCandidate();
            sb.append("### Step ").append(step.getStepNumber()).append(": ").append(step.getTitle()).append("\n\n");

            sb.append(step.getExplanation().toString());

            com.amie.refactor.forecast.BlastRadiusInfo br = c.getBlastRadius();
            if (br != null) {
                sb.append("#### Effort & Blast Radius Analysis\n\n");
                sb.append("- **Risk Classification:** ").append(br.getRiskBadge()).append("\n");
                sb.append("- **AST Coupling:** Fan-In = ").append(br.getFanIn()).append(" (callers), Fan-Out = ").append(br.getFanOut()).append(" (callees)\n");
                if (!br.getAffectedCallers().isEmpty()) {
                    sb.append("- **Affected Callers:** `").append(String.join("`, `", br.getAffectedCallers())).append("`\n");
                }
                sb.append("- **Ripple Rationale:** ").append(br.getRiskRationale()).append("\n\n");
            }

            com.amie.refactor.forecast.MetricForecast fc = c.getMetricForecast();
            if (fc != null) {
                sb.append("#### Simulated \"What-If\" Metric Forecast\n\n");
                sb.append("| Metric | Before | Predicted After | Change |\n");
                sb.append("|:---|:---:|:---:|:---:|\n");
                sb.append(String.format("| **WMC (Complexity)** | %d | %d | %s |\n",
                        fc.getWmcBefore(), fc.getWmcAfter(), fc.getWmcReductionPct() > 0 ? "-" + fc.getWmcReductionPct() + "%" : "Neutral"));
                sb.append(String.format("| **LCOM (Cohesion)** | %d | %d | %s |\n",
                        fc.getLcomBefore(), fc.getLcomAfter(), fc.getLcomReductionPct() > 0 ? "-" + fc.getLcomReductionPct() + "%" : "Neutral"));
                sb.append(String.format("| **CBO (Coupling)** | %d | %d | %+d |\n",
                        fc.getCboBefore(), fc.getCboAfter(), (fc.getCboAfter() - fc.getCboBefore())));
                if (fc.getParamCountBefore() > 0) {
                    sb.append(String.format("| **Parameters** | %d | %d | %d eliminated |\n",
                            fc.getParamCountBefore(), fc.getParamCountAfter(), (fc.getParamCountBefore() - fc.getParamCountAfter())));
                }
                sb.append("\n*Forecast Insight:* ").append(fc.getForecastSummary()).append("\n\n");
            }

            com.amie.refactor.diff.RefactoringDiff diff = c.getDiffPreview();
            if (diff != null) {
                sb.append("### Code Refactoring Preview\n\n");
                sb.append("**Change Summary:** ").append(diff.getExplanationSummary()).append("\n\n");
                sb.append("<details open>\n<summary><b>Unified Code Diff (.patch)</b></summary>\n\n");
                sb.append("```diff\n").append(diff.getUnifiedDiff()).append("```\n\n");
                sb.append("</details>\n\n");
                sb.append("**Refactored Target Code:**\n\n");
                sb.append("```java\n").append(diff.getAfterCode()).append("\n```\n\n");
            }

            sb.append("\n---\n\n");
        }

        Files.writeString(outputFile, sb.toString());
        return outputFile;
    }

    private double findScore(List<RefactoringCandidate> list, String cid) {
        if (list == null) return 0.0;
        return list.stream()
                .filter(c -> c.getCandidateId().equals(cid))
                .findFirst()
                .map(RefactoringCandidate::getPriorityScore)
                .orElse(0.0);
    }
}
