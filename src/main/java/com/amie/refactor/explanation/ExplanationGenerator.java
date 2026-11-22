package com.amie.refactor.explanation;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.Dependency;
import com.amie.refactor.model.dependency.DependencyAnalysisResult;
import com.amie.refactor.model.dependency.MethodStructure;
import com.amie.refactor.planning.RefactoringCandidate;
import com.amie.refactor.planning.ScoreBreakdown;
import com.amie.refactor.planning.WorkflowPlan;

import java.util.*;

/**
 * Deterministic, code-grounded explanation generator.
 * Implements Objective 4 of the thesis proposal.
 *
 * <p>Produces transparent explanations grounded in real source code facts,
 * verified CK metrics, AST structures, and dependency graphs.</p>
 */
public class ExplanationGenerator {

    private final com.amie.refactor.diff.DiffPreviewGenerator diffGenerator =
            new com.amie.refactor.diff.DiffPreviewGenerator();

    public List<WorkflowStep> generateWorkflowSteps(
            WorkflowPlan plan,
            DependencyAnalysisResult dependencyAnalysis) {

        Objects.requireNonNull(plan, "plan must not be null");

        List<WorkflowStep> steps = new ArrayList<>();
        List<RefactoringCandidate> ordered = plan.getOrderedCandidates();

        // Index candidate positions in workflow for ordering explanations
        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            positionMap.put(ordered.get(i).getCandidateId(), i + 1);
        }

        for (int i = 0; i < ordered.size(); i++) {
            int stepNum = i + 1;
            RefactoringCandidate candidate = ordered.get(i);

            String title = String.format("%s in %s%s",
                    candidate.getRecommendedAction().getActionName(),
                    candidate.getClassName(),
                    (candidate.getMethodName() != null && !candidate.getMethodName().equalsIgnoreCase("N/A")
                            ? "." + candidate.getMethodName() + "()"
                            : ""));

            if (candidate.getDiffPreview() == null) {
                candidate.setDiffPreview(diffGenerator.generateDiff(candidate));
            }

            StepExplanation explanation = buildExplanation(
                    stepNum, candidate, plan, dependencyAnalysis, positionMap);

            steps.add(new WorkflowStep(stepNum, candidate, title, explanation));
        }

        return steps;
    }

    private StepExplanation buildExplanation(
            int stepNumber,
            RefactoringCandidate candidate,
            WorkflowPlan plan,
            DependencyAnalysisResult dependencyAnalysis,
            Map<String, Integer> positionMap) {

        String whatAndWhere = generateWhatAndWhere(candidate);
        String whyRecommended = generateWhyRecommended(candidate);
        String whyPrioritized = generateWhyPrioritized(candidate, plan);
        String whyInThisSequence = generateWhyInThisSequence(stepNumber, candidate, plan, positionMap);
        List<String> guidance = generateActionableGuidance(candidate);

        return new StepExplanation(
                whatAndWhere,
                whyRecommended,
                whyPrioritized,
                whyInThisSequence,
                guidance);
    }

    private String generateWhatAndWhere(RefactoringCandidate c) {
        CodeSmell smell = c.getOriginalSmell();
        String targetName = c.getClassName();
        if (c.getMethodName() != null && !c.getMethodName().equalsIgnoreCase("N/A")) {
            targetName += "." + c.getMethodName() + "()";
        }

        return String.format(
                "Target: `%s` located in `%s` (lines %d to %d).\n" +
                "Detected Code Smell: **%s** (detected via PMD rule `%s`, severity rating: %s).\n" +
                "Recommended Action: **%s**.",
                targetName,
                c.getFilePath(),
                c.getStartLine(),
                c.getEndLine(),
                c.getSmellType(),
                smell.getRuleName(),
                smell.getSeverity(),
                c.getRecommendedAction().getActionName());
    }

    private String generateWhyRecommended(RefactoringCandidate c) {
        StringBuilder sb = new StringBuilder();
        ClassMetrics cm = c.getClassMetrics();
        MethodStructure ms = c.getMethodStructure();
        ClassStructure cs = c.getClassStructure();

        switch (c.getSmellType()) {
            case LONG_METHOD -> {
                int lines = ms != null ? ms.getLineCount() : (c.getEndLine() - c.getStartLine() + 1);
                int cc = ms != null ? ms.getCyclomaticComplexity() : 1;
                sb.append(String.format(
                        "The method contains %d lines of code and a McCabe cyclomatic complexity of %d. " +
                        "This excessive length groups distinct execution steps together, increasing cognitive load, " +
                        "defect probability, and making unit testing difficult.",
                        lines, cc));
                if (ms != null && !ms.getCalledMethods().isEmpty()) {
                    sb.append(String.format(" The method invokes %d sub-calls including: %s.",
                            ms.getCalledMethods().size(),
                            String.join(", ", ms.getCalledMethods().stream().limit(5).toList())));
                }
            }
            case GOD_CLASS -> {
                int methodCount = cs != null ? cs.getMethods().size() : 0;
                int fieldCount = cs != null ? cs.getFields().size() : 0;
                int wmc = cm != null && cm.getWmc() != null ? cm.getWmc() : 0;
                int lcom = cm != null && cm.getLcom() != null ? cm.getLcom() : 0;
                sb.append(String.format(
                        "Class `%s` aggregates excessive responsibilities violating the Single Responsibility Principle. " +
                        "Structural analysis identified %d methods and %d state fields. " +
                        "CK metrics confirm high complexity and low cohesion (WMC=%d, LCOM=%d).",
                        c.getClassName(), methodCount, fieldCount, wmc, lcom));
            }
            case FEATURE_ENVY -> {
                int cbo = cm != null && cm.getCbo() != null ? cm.getCbo() : 0;
                sb.append(String.format(
                        "The class/method exhibits strong coupling to external classes rather than its own data members (measured CBO=%d).",
                        cbo));
                if (cs != null && !cs.getDependentClassNames().isEmpty()) {
                    sb.append(String.format(" It interacts extensively with foreign classes: %s. " +
                            "This violates encapsulation by depending on external implementations.",
                            String.join(", ", cs.getDependentClassNames())));
                }
            }
            case DATA_CLUMPS -> {
                int paramCount = ms != null ? ms.getParameterCount() : 0;
                sb.append(String.format(
                        "A repeated cluster of related parameters (%d parameters) is passed across method boundaries. " +
                        "Data clumps indicate missing domain abstractions that should be encapsulated in a dedicated value object.",
                        paramCount));
            }
        }

        if (cm != null) {
            sb.append(String.format(" Overall class metrics: CBO=%d, WMC=%d, LCOM=%d.",
                    cm.getCbo() != null ? cm.getCbo() : 0,
                    cm.getWmc() != null ? cm.getWmc() : 0,
                    cm.getLcom() != null ? cm.getLcom() : 0));
        }

        return sb.toString();
    }

    private String generateWhyPrioritized(RefactoringCandidate c, WorkflowPlan plan) {
        ScoreBreakdown sb = c.getScoreBreakdown();
        if (sb == null) {
            return "Prioritized based on detected code smell severity.";
        }

        return String.format(
                "Calculated Priority Score: **%.3f / 1.000** under selected goal **'%s'**.\n" +
                "Scoring Factor Breakdown:\n" +
                "- **Severity Score (S_i = %.2f, weight = %.2f)**: %s\n" +
                "- **Metric Impact (M_i = %.2f, weight = %.2f)**: %s\n" +
                "- **Dependency Ripple Impact (D_i = %.2f, weight = %.2f)**: %s\n" +
                "- **Estimated Improvement Benefit (B_i = %.2f, weight = %.2f)**: %s",
                sb.getFinalPriorityScore(),
                plan.getQualityGoal().getDisplayName(),
                sb.getSeverityScore(), sb.getSeverityWeight(), sb.getSeverityRationale(),
                sb.getMetricImpactScore(), sb.getMetricWeight(), sb.getMetricRationale(),
                sb.getDependencyImpactScore(), sb.getDependencyWeight(), sb.getDependencyRationale(),
                sb.getEstimatedBenefitScore(), sb.getBenefitWeight(), sb.getBenefitRationale());
    }

    private String generateWhyInThisSequence(
            int stepNumber,
            RefactoringCandidate c,
            WorkflowPlan plan,
            Map<String, Integer> positionMap) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Positioned at **Step %d** in the refactoring sequence. ", stepNumber));

        if (c.getPrerequisiteIds().isEmpty()) {
            sb.append("This task has no unsatisfied prerequisites; ");
            if (stepNumber == 1) {
                sb.append("it earned the highest priority score among immediately executable candidates.");
            } else {
                sb.append("it was scheduled according to its relative priority score among available tasks.");
            }
        } else {
            List<String> prereqSteps = new ArrayList<>();
            for (String pid : c.getPrerequisiteIds()) {
                Integer pos = positionMap.get(pid);
                prereqSteps.add(pid + (pos != null ? " (Step " + pos + ")" : ""));
            }
            sb.append(String.format(
                    "Execution was constrained by prerequisite task(s): %s. " +
                    "Performing those prerequisite changes first ensures stable interface contracts and prevents broken call-sites.",
                    String.join(", ", prereqSteps)));
        }

        // Check if this task is a prerequisite for subsequent steps
        List<String> downstreamSteps = new ArrayList<>();
        for (RefactoringCandidate other : plan.getOrderedCandidates()) {
            if (other.getPrerequisiteIds().contains(c.getCandidateId())) {
                Integer pos = positionMap.get(other.getCandidateId());
                downstreamSteps.add(other.getCandidateId() + (pos != null ? " (Step " + pos + ")" : ""));
            }
        }

        if (!downstreamSteps.isEmpty()) {
            sb.append(String.format(
                    " Completing this step unlocks subsequent scheduled tasks: %s.",
                    String.join(", ", downstreamSteps)));
        }

        return sb.toString();
    }

    private List<String> generateActionableGuidance(RefactoringCandidate c) {
        List<String> steps = new ArrayList<>();
        switch (c.getSmellType()) {
            case LONG_METHOD -> {
                steps.add("Review the statements within lines " + c.getStartLine() + " to " + c.getEndLine() + " and identify cohesive logical sub-blocks.");
                steps.add("Use your IDE's automated refactoring (e.g. Refactor -> Extract Method) to extract each cohesive block into private helper methods.");
                steps.add("Ensure extracted method names clearly express their intention without requiring inline comments.");
                steps.add("Execute existing unit tests to confirm zero behavioral regressions.");
                steps.add("Re-run the planner analysis to verify that the method line count and McCabe complexity have decreased.");
            }
            case GOD_CLASS -> {
                steps.add("Analyze class field access patterns and identify subsets of methods that operate on distinct fields.");
                steps.add("Create new domain classes for each isolated responsibility (e.g. separate billing, student registration, or reporting).");
                steps.add("Move relevant fields and corresponding methods to the newly created classes.");
                steps.add("Inject references to the new classes into the original class to maintain backward compatibility if needed.");
                steps.add("Run the test suite to verify that system contracts remain intact.");
            }
            case FEATURE_ENVY -> {
                steps.add("Identify the external class whose data is accessed most heavily by this method.");
                steps.add("Evaluate if moving the method into the target class enhances information hiding and cohesion.");
                steps.add("Use IDE Refactor -> Move Method to relocate the logic, adjusting caller parameters accordingly.");
                steps.add("Validate integration tests and confirm coupling (CBO) is reduced.");
            }
            case DATA_CLUMPS -> {
                steps.add("Identify the cluster of recurring parameters passed together across method signatures.");
                steps.add("Create a new value class / parameter object (or Java record) to group those fields.");
                steps.add("Update method signatures to accept the new parameter object rather than individual primitive fields.");
                steps.add("Verify compilation and re-run unit tests to confirm proper encapsulation.");
            }
        }
        return steps;
    }
}
