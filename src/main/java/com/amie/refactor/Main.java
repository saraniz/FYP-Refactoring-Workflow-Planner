package com.amie.refactor;

import com.amie.refactor.analysis.MetricsAnalyzer;
import com.amie.refactor.analysis.PmdResultParser;
import com.amie.refactor.analysis.PmdRunner;
import com.amie.refactor.analysis.SmellMetricsIntegrator;
import com.amie.refactor.analysis.dependency.JavaParserDependencyAnalyzer;
import com.amie.refactor.explanation.ExplanationGenerator;
import com.amie.refactor.explanation.WorkflowStep;
import com.amie.refactor.model.*;
import com.amie.refactor.model.dependency.DependencyAnalysisResult;
import com.amie.refactor.planning.*;
import com.amie.refactor.reporting.JsonExporter;
import com.amie.refactor.reporting.MarkdownReportExporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    private static final Path SAMPLE_PROJECT =
            Path.of("sample-projects/smell-test-project1");

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("  Code-Aware Refactoring Workflow Planner");
        System.out.println("  Goal-Oriented Prioritization & Explainable Step Guidance");
        System.out.println("==========================================================\n");

        Path projectDir = args.length > 0 ? Path.of(args[0]) : SAMPLE_PROJECT;

        if (!Files.exists(projectDir)) {
            System.err.println("Target project directory not found at: "
                    + projectDir.toAbsolutePath());
            return;
        }

        // =========================================================================
        // Stage 1: Code Smell Detection (PMD Module)
        // =========================================================================
        List<CodeSmell> smells;
        try {
            System.out.println("[Stage 1] Executing PMD smell detection on: " + projectDir.toAbsolutePath());
            new PmdRunner().runPmd(projectDir);

            Path pmdXmlPath = projectDir.resolve("target/pmd.xml");
            System.out.println("          Parsing PMD output from: " + pmdXmlPath.getFileName());
            smells = new PmdResultParser().parse(pmdXmlPath);
            System.out.println("          Detected " + smells.size() + " code smells across project.\n");
        } catch (Exception e) {
            System.err.println("Error running PMD analysis pipeline: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // =========================================================================
        // Stage 2: Software Metrics Analysis (CK Tool Module)
        // =========================================================================
        Path sourceDirectory = projectDir.resolve("src/main/java");
        if (!Files.exists(sourceDirectory)) {
            sourceDirectory = projectDir; // fallback if flat project
        }
        System.out.println("[Stage 2] Running CK Tool metrics analysis on: " + sourceDirectory.toAbsolutePath());
        MetricsAnalysisResult metricsResult = new MetricsAnalyzer().analyze(sourceDirectory);
        System.out.println("          Metrics status: " + metricsResult.getStatus()
                + " (" + metricsResult.getClassMetrics().size() + " classes measured).\n");

        CombinedAnalysisResult combined =
                new SmellMetricsIntegrator().combine(projectDir, smells, metricsResult);

        // =========================================================================
        // Stage 3: Local Dependency & Code Structure Analysis (JavaParser Module)
        // =========================================================================
        System.out.println("[Stage 3] Running JavaParser AST structural & dependency analysis...");
        JavaParserDependencyAnalyzer dependencyAnalyzer = new JavaParserDependencyAnalyzer();
        DependencyAnalysisResult dependencyResult = dependencyAnalyzer.analyze(sourceDirectory);
        System.out.println("          AST parsed " + dependencyResult.getClasses().size()
                + " classes, identified " + dependencyResult.getAllDependencies().size() + " dependency relations.\n");

        // =========================================================================
        // Stage 4: Goal-Oriented Workflow Planning Engine
        // =========================================================================
        System.out.println("[Stage 4] Generating Goal-Oriented Refactoring Workflows...");
        WorkflowPlanner planner = new WorkflowPlanner();

        // Plan for all three developer quality goals
        WorkflowPlan maintainabilityPlan = planner.plan(combined, dependencyResult, QualityGoal.IMPROVE_MAINTAINABILITY);
        WorkflowPlan complexityPlan = planner.plan(combined, dependencyResult, QualityGoal.REDUCE_COMPLEXITY);
        WorkflowPlan balancedPlan = planner.plan(combined, dependencyResult, QualityGoal.BALANCED_IMPROVEMENT);

        Map<String, List<RefactoringCandidate>> comparativeRankings = new LinkedHashMap<>();
        comparativeRankings.put("IMPROVE_MAINTAINABILITY", maintainabilityPlan.getOrderedCandidates());
        comparativeRankings.put("REDUCE_COMPLEXITY", complexityPlan.getOrderedCandidates());
        comparativeRankings.put("BALANCED_IMPROVEMENT", balancedPlan.getOrderedCandidates());

        System.out.println("          Generated workflows for Maintainability, Complexity, and Balanced goals.");
        System.out.println("          Enforced " + maintainabilityPlan.getDependencyConstraintCount() + " ordering constraints in DAG.\n");

        // =========================================================================
        // Stage 5: Code-Aware Explanation Generation
        // =========================================================================
        System.out.println("[Stage 5] Synthesizing Code-Aware Explanations grounded in AST facts...");
        ExplanationGenerator explanationGenerator = new ExplanationGenerator();

        // Primary demonstrated plan: Maintainability
        List<WorkflowStep> maintainabilitySteps = explanationGenerator.generateWorkflowSteps(maintainabilityPlan, dependencyResult);
        List<WorkflowStep> complexitySteps = explanationGenerator.generateWorkflowSteps(complexityPlan, dependencyResult);
        List<WorkflowStep> balancedSteps = explanationGenerator.generateWorkflowSteps(balancedPlan, dependencyResult);

        System.out.println("          Generated grounded explanations for all " + maintainabilitySteps.size() + " workflow steps.\n");

        // =========================================================================
        // Stage 6: Reporting & Exporting Results
        // =========================================================================
        System.out.println("[Stage 6] Exporting structured results & reports...");
        try {
            JsonExporter jsonExporter = new JsonExporter();
            Path jsonOutMaint = jsonExporter.export(
                    maintainabilityPlan, maintainabilitySteps, Path.of("results/workflow-plan-maintainability.json"));
            Path jsonOutComp = jsonExporter.export(
                    complexityPlan, complexitySteps, Path.of("results/workflow-plan-complexity.json"));
            Path jsonOutBal = jsonExporter.export(
                    balancedPlan, balancedSteps, Path.of("results/workflow-plan-balanced.json"));

            MarkdownReportExporter mdExporter = new MarkdownReportExporter();
            Path mdOut = mdExporter.export(
                    maintainabilityPlan, maintainabilitySteps, comparativeRankings, Path.of("results/refactoring-plan-report.md"));

            com.amie.refactor.reporting.HtmlDashboardExporter htmlExporter =
                    new com.amie.refactor.reporting.HtmlDashboardExporter();
            Path htmlOut = htmlExporter.export(
                    maintainabilityPlan, maintainabilitySteps,
                    complexityPlan, complexitySteps,
                    balancedPlan, balancedSteps,
                    Path.of("results/dashboard.html"));

            System.out.println("          Exported JSON plans: " + jsonOutMaint + ", " + jsonOutComp + ", " + jsonOutBal);
            System.out.println("          Exported Markdown report: " + mdOut);
            System.out.println("          Exported Interactive Dashboard: " + htmlOut + "\n");
        } catch (Exception e) {
            System.err.println("Warning: failed to export reports: " + e.getMessage());
        }

        // =========================================================================
        // CLI Summary Presentation
        // =========================================================================
        System.out.println("==========================================================");
        System.out.println("  PRIORITIZED REFACTORING WORKFLOW (" + maintainabilityPlan.getQualityGoal().getDisplayName() + ")");
        System.out.println("==========================================================");

        for (WorkflowStep step : maintainabilitySteps) {
            RefactoringCandidate c = step.getCandidate();
            System.out.printf("Step %d [Priority %.3f] - %s%n",
                    step.getStepNumber(), c.getPriorityScore(), step.getTitle());
            System.out.println("  Location : " + c.getFilePath() + " (lines " + c.getStartLine() + "-" + c.getEndLine() + ")");
            System.out.println("  Smell    : " + c.getSmellType() + " | Action: " + c.getRecommendedAction().getActionName());
            ScoreBreakdown sb = c.getScoreBreakdown();
            if (sb != null) {
                System.out.printf("  Scores   : Severity=%.2f (w=%.2f), Metric=%.2f (w=%.2f), Dep=%.2f (w=%.2f), Benefit=%.2f (w=%.2f)%n",
                        sb.getSeverityScore(), sb.getSeverityWeight(),
                        sb.getMetricImpactScore(), sb.getMetricWeight(),
                        sb.getDependencyImpactScore(), sb.getDependencyWeight(),
                        sb.getEstimatedBenefitScore(), sb.getBenefitWeight());
            }
            if (!c.getPrerequisiteIds().isEmpty()) {
                System.out.println("  Prereqs  : Preceded by " + String.join(", ", c.getPrerequisiteIds()));
            }
            System.out.println("----------------------------------------------------------");
        }

        System.out.println("\nComparative Goal Summary (Top Priority Shifts):");
        System.out.printf("%-10s %-25s %-16s %-16s %-16s%n", "Candidate", "Target", "Maintainability", "Complexity", "Balanced");
        System.out.println("-----------------------------------------------------------------------------------");
        for (WorkflowStep step : maintainabilitySteps) {
            String cid = step.getCandidate().getCandidateId();
            String target = step.getCandidate().getClassName();
            double sMaint = step.getCandidate().getPriorityScore();
            double sComp = complexityPlan.getOrderedCandidates().stream()
                    .filter(c -> c.getCandidateId().equals(cid)).findFirst().map(RefactoringCandidate::getPriorityScore).orElse(0.0);
            double sBal = balancedPlan.getOrderedCandidates().stream()
                    .filter(c -> c.getCandidateId().equals(cid)).findFirst().map(RefactoringCandidate::getPriorityScore).orElse(0.0);
            System.out.printf("%-10s %-25s %-16.3f %-16.3f %-16.3f%n", cid, target, sMaint, sComp, sBal);
        }
        System.out.println("==========================================================\n");
    }
}
