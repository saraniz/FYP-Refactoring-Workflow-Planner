package com.amie.refactor.reporting;

import com.amie.refactor.explanation.WorkflowStep;
import com.amie.refactor.planning.RefactoringCandidate;
import com.amie.refactor.planning.ScoreBreakdown;
import com.amie.refactor.planning.WorkflowPlan;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Exports workflow plans and explanations into structured JSON for research evaluation
 * and downstream Python/Pandas processing.
 */
public class JsonExporter {

    private final Gson gson;

    public JsonExporter() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public Path export(WorkflowPlan plan, List<WorkflowStep> steps, Path outputFile) throws IOException {
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(steps, "steps must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");

        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("targetDirectory", plan.getTargetDirectory());
        root.put("plannedAt", plan.getPlannedAt());
        root.put("qualityGoal", plan.getQualityGoal().name());
        root.put("qualityGoalDisplayName", plan.getQualityGoal().getDisplayName());

        Map<String, Double> weightsMap = new LinkedHashMap<>();
        weightsMap.put("w_severity", plan.getGoalWeights().getSeverityWeight());
        weightsMap.put("w_metric", plan.getGoalWeights().getMetricWeight());
        weightsMap.put("w_dependency", plan.getGoalWeights().getDependencyWeight());
        weightsMap.put("w_benefit", plan.getGoalWeights().getBenefitWeight());
        root.put("weights", weightsMap);

        root.put("totalCandidates", plan.getCandidateCount());
        root.put("dependencyConstraintsCount", plan.getDependencyConstraintCount());
        root.put("warnings", plan.getWarnings());

        List<Map<String, Object>> stepsList = new ArrayList<>();
        for (WorkflowStep step : steps) {
            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("stepNumber", step.getStepNumber());
            sMap.put("title", step.getTitle());

            RefactoringCandidate c = step.getCandidate();
            sMap.put("candidateId", c.getCandidateId());
            sMap.put("smellType", c.getSmellType().name());
            sMap.put("recommendedAction", c.getRecommendedAction().name());
            sMap.put("className", c.getClassName());
            sMap.put("fullyQualifiedClassName", c.getFullyQualifiedClassName());
            sMap.put("methodName", c.getMethodName());
            sMap.put("filePath", c.getFilePath());
            sMap.put("startLine", c.getStartLine());
            sMap.put("endLine", c.getEndLine());
            sMap.put("priorityScore", c.getPriorityScore());
            sMap.put("prerequisites", c.getPrerequisiteIds());

            ScoreBreakdown sb = c.getScoreBreakdown();
            if (sb != null) {
                Map<String, Object> sbMap = new LinkedHashMap<>();
                sbMap.put("severityScore", sb.getSeverityScore());
                sbMap.put("metricImpactScore", sb.getMetricImpactScore());
                sbMap.put("dependencyImpactScore", sb.getDependencyImpactScore());
                sbMap.put("estimatedBenefitScore", sb.getEstimatedBenefitScore());
                sbMap.put("severityRationale", sb.getSeverityRationale());
                sbMap.put("metricRationale", sb.getMetricRationale());
                sbMap.put("dependencyRationale", sb.getDependencyRationale());
                sbMap.put("benefitRationale", sb.getBenefitRationale());
                sMap.put("scoreBreakdown", sbMap);
            }

            com.amie.refactor.diff.RefactoringDiff diff = c.getDiffPreview();
            if (diff != null) {
                Map<String, Object> diffMap = new LinkedHashMap<>();
                diffMap.put("actionName", diff.getAction().getActionName());
                diffMap.put("summary", diff.getExplanationSummary());
                diffMap.put("beforeCode", diff.getBeforeCode());
                diffMap.put("afterCode", diff.getAfterCode());
                diffMap.put("unifiedDiff", diff.getUnifiedDiff());
                sMap.put("diffPreview", diffMap);
            }

            com.amie.refactor.forecast.MetricForecast forecast = c.getMetricForecast();
            if (forecast != null) {
                Map<String, Object> fcMap = new LinkedHashMap<>();
                fcMap.put("wmcBefore", forecast.getWmcBefore());
                fcMap.put("wmcAfter", forecast.getWmcAfter());
                fcMap.put("wmcReductionPct", forecast.getWmcReductionPct());
                fcMap.put("lcomBefore", forecast.getLcomBefore());
                fcMap.put("lcomAfter", forecast.getLcomAfter());
                fcMap.put("lcomReductionPct", forecast.getLcomReductionPct());
                fcMap.put("cboBefore", forecast.getCboBefore());
                fcMap.put("cboAfter", forecast.getCboAfter());
                fcMap.put("locBefore", forecast.getLocBefore());
                fcMap.put("locAfter", forecast.getLocAfter());
                fcMap.put("paramCountBefore", forecast.getParamCountBefore());
                fcMap.put("paramCountAfter", forecast.getParamCountAfter());
                fcMap.put("forecastSummary", forecast.getForecastSummary());
                sMap.put("metricForecast", fcMap);
            }

            com.amie.refactor.forecast.BlastRadiusInfo blastRadius = c.getBlastRadius();
            if (blastRadius != null) {
                Map<String, Object> brMap = new LinkedHashMap<>();
                brMap.put("fanIn", blastRadius.getFanIn());
                brMap.put("fanOut", blastRadius.getFanOut());
                brMap.put("riskCategory", blastRadius.getRiskCategoryName());
                brMap.put("riskBadge", blastRadius.getRiskBadge());
                brMap.put("colorHex", blastRadius.getColorHex());
                brMap.put("affectedCallers", blastRadius.getAffectedCallers());
                brMap.put("riskRationale", blastRadius.getRiskRationale());
                sMap.put("blastRadius", brMap);
            }

            Map<String, Object> expMap = new LinkedHashMap<>();
            expMap.put("whatAndWhere", step.getExplanation().getWhatAndWhere());
            expMap.put("whyRecommended", step.getExplanation().getWhyRecommended());
            expMap.put("whyPrioritized", step.getExplanation().getWhyPrioritized());
            expMap.put("whyInThisSequence", step.getExplanation().getWhyInThisSequence());
            expMap.put("actionableGuidance", step.getExplanation().getActionableGuidance());
            sMap.put("explanation", expMap);

            stepsList.add(sMap);
        }
        root.put("steps", stepsList);

        String json = gson.toJson(root);
        Files.writeString(outputFile, json);
        return outputFile;
    }
}
