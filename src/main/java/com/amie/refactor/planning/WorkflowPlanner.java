package com.amie.refactor.planning;

import com.amie.refactor.model.ClassAnalysis;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.CombinedAnalysisResult;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.DependencyAnalysisResult;
import com.amie.refactor.model.dependency.MethodStructure;

import java.util.*;

/**
 * Goal-oriented workflow planning engine.
 * Implements Objective 3 of the thesis proposal.
 *
 * <p>Synthesizes code smell findings, CK software metrics, and JavaParser dependency
 * analysis into prioritized, dependency-aware refactoring workflows adapted to developer goals.</p>
 */
public class WorkflowPlanner {

    private final PriorityScorer priorityScorer;
    private final com.amie.refactor.forecast.MetricForecastSimulator forecastSimulator;

    public WorkflowPlanner() {
        this(new PriorityScorer(), new com.amie.refactor.forecast.MetricForecastSimulator());
    }

    public WorkflowPlanner(PriorityScorer priorityScorer) {
        this(priorityScorer, new com.amie.refactor.forecast.MetricForecastSimulator());
    }

    public WorkflowPlanner(PriorityScorer priorityScorer, com.amie.refactor.forecast.MetricForecastSimulator forecastSimulator) {
        this.priorityScorer = Objects.requireNonNull(priorityScorer, "priorityScorer must not be null");
        this.forecastSimulator = Objects.requireNonNull(forecastSimulator, "forecastSimulator must not be null");
    }

    public WorkflowPlan plan(
            CombinedAnalysisResult combinedAnalysis,
            DependencyAnalysisResult dependencyAnalysis,
            QualityGoal goal) {

        return plan(combinedAnalysis, dependencyAnalysis, goal, GoalWeights.forGoal(goal));
    }

    public WorkflowPlan plan(
            CombinedAnalysisResult combinedAnalysis,
            DependencyAnalysisResult dependencyAnalysis,
            QualityGoal goal,
            GoalWeights weights) {

        Objects.requireNonNull(combinedAnalysis, "combinedAnalysis must not be null");
        Objects.requireNonNull(goal, "goal must not be null");
        Objects.requireNonNull(weights, "weights must not be null");

        List<String> warnings = new ArrayList<>();
        List<RefactoringCandidate> candidates = new ArrayList<>();
        int candidateSeq = 1;

        // Step 1: Create normalized RefactoringCandidates from all detected smells
        for (ClassAnalysis classAnalysis : combinedAnalysis.getClasses()) {
            String fqn = classAnalysis.getFullyQualifiedClassName();
            ClassStructure classStruct = dependencyAnalysis != null
                    ? dependencyAnalysis.getClassStructure(fqn)
                    : null;

            for (CodeSmell smell : classAnalysis.getCodeSmells()) {
                String candidateId = String.format("RC-%02d", candidateSeq++);
                RefactoringAction action = mapSmellToAction(smell.getSmellType());

                MethodStructure methodStruct = null;
                if (classStruct != null && smell.getMethodName() != null && !smell.getMethodName().equalsIgnoreCase("N/A")) {
                    methodStruct = classStruct.findMethod(smell.getMethodName()).orElse(null);
                }

                RefactoringCandidate candidate = new RefactoringCandidate(
                        candidateId,
                        smell,
                        action,
                        classAnalysis.getMetrics(),
                        methodStruct,
                        classStruct);

                // Step 2: Calculate Priority Score for candidate under chosen goal & weights
                ScoreBreakdown scoreBreakdown = priorityScorer.calculateScore(candidate, goal, weights);
                candidate.setScoreBreakdown(scoreBreakdown);

                // Simulate "What-If" metric forecast and evaluate effort / blast radius
                candidate.setMetricForecast(forecastSimulator.simulate(candidate));
                candidate.setBlastRadius(forecastSimulator.evaluateBlastRadius(candidate));

                candidates.add(candidate);
            }
        }

        if (candidates.isEmpty()) {
            warnings.add("No refactoring candidates generated (no smells detected in input).");
            return new WorkflowPlan(
                    combinedAnalysis.getTargetDirectory(),
                    goal,
                    weights,
                    Collections.emptyList(),
                    0,
                    warnings);
        }

        // Step 3: Establish dependency and precedence constraints in the DAG
        RefactoringDependencyGraph graph = new RefactoringDependencyGraph(candidates);

        // Map candidates by class for intra-class constraint checks
        Map<String, List<RefactoringCandidate>> candidatesByClass = new HashMap<>();
        for (RefactoringCandidate c : candidates) {
            candidatesByClass.computeIfAbsent(c.getFullyQualifiedClassName(), k -> new ArrayList<>()).add(c);
        }

        // Intra-class Constraint 1: Long Method must precede God Class (extract methods before extracting class)
        for (List<RefactoringCandidate> classCandidates : candidatesByClass.values()) {
            List<RefactoringCandidate> longMethods = classCandidates.stream()
                    .filter(c -> c.getSmellType() == SmellType.LONG_METHOD)
                    .toList();
            List<RefactoringCandidate> godClasses = classCandidates.stream()
                    .filter(c -> c.getSmellType() == SmellType.GOD_CLASS)
                    .toList();

            for (RefactoringCandidate lm : longMethods) {
                for (RefactoringCandidate gc : godClasses) {
                    graph.addPrecedenceConstraint(
                            lm.getCandidateId(),
                            gc.getCandidateId(),
                            "Decompose Long Method into cohesive units before extracting class responsibilities");
                }
            }

            // Intra-class Constraint 2: Data Clumps must precede Extract Method / God Class
            List<RefactoringCandidate> dataClumps = classCandidates.stream()
                    .filter(c -> c.getSmellType() == SmellType.DATA_CLUMPS)
                    .toList();

            for (RefactoringCandidate dc : dataClumps) {
                for (RefactoringCandidate lm : longMethods) {
                    graph.addPrecedenceConstraint(
                            dc.getCandidateId(),
                            lm.getCandidateId(),
                            "Introduce Parameter Object to unify parameter clumps before method refactoring");
                }
                for (RefactoringCandidate gc : godClasses) {
                    graph.addPrecedenceConstraint(
                            dc.getCandidateId(),
                            gc.getCandidateId(),
                            "Introduce Parameter Object before decomposing class structure");
                }
            }
        }

        // Inter-class Constraint 3: Callee / Independent Service refactoring precedes Caller
        if (dependencyAnalysis != null) {
            for (RefactoringCandidate c1 : candidates) {
                for (RefactoringCandidate c2 : candidates) {
                    if (c1 == c2) continue;
                    String class1 = c1.getFullyQualifiedClassName();
                    String class2 = c2.getFullyQualifiedClassName();

                    if (!class1.equals(class2)) {
                        // If class1 calls class2, then class2 (callee) should be stabilized before class1 (caller)
                        if (dependencyAnalysis.hasDirectDependency(class1, class2)) {
                            // Don't add if reverse constraint already exists
                            graph.addPrecedenceConstraint(
                                    c2.getCandidateId(),
                                    c1.getCandidateId(),
                                    "Stabilize depended-upon service (" + c2.getClassName() + ") before refactoring client (" + c1.getClassName() + ")");
                        }
                    }
                }
            }
        }

        warnings.addAll(graph.getWarnings());

        // Step 4: Topological ordering with Max-Priority Queue
        List<RefactoringCandidate> orderedCandidates = graph.getTopologicalOrder();

        return new WorkflowPlan(
                combinedAnalysis.getTargetDirectory(),
                goal,
                weights,
                orderedCandidates,
                graph.getEdgeCount(),
                warnings);
    }

    private RefactoringAction mapSmellToAction(SmellType smellType) {
        return switch (smellType) {
            case LONG_METHOD -> RefactoringAction.EXTRACT_METHOD;
            case GOD_CLASS -> RefactoringAction.EXTRACT_CLASS;
            case FEATURE_ENVY -> RefactoringAction.MOVE_METHOD;
            case DATA_CLUMPS -> RefactoringAction.INTRODUCE_PARAMETER_OBJECT;
        };
    }
}
