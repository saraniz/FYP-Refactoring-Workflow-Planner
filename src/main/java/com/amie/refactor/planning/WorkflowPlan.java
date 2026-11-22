package com.amie.refactor.planning;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Result of the goal-oriented workflow planning engine.
 * Contains the ordered sequence of refactoring candidates, scoring weights,
 * and dependency graph properties.
 */
public class WorkflowPlan {

    private final String targetDirectory;
    private final QualityGoal qualityGoal;
    private final GoalWeights goalWeights;
    private final List<RefactoringCandidate> orderedCandidates;
    private final int dependencyConstraintCount;
    private final List<String> warnings;
    private final String plannedAt;

    public WorkflowPlan(
            String targetDirectory,
            QualityGoal qualityGoal,
            GoalWeights goalWeights,
            List<RefactoringCandidate> orderedCandidates,
            int dependencyConstraintCount,
            List<String> warnings) {

        this.targetDirectory = targetDirectory;
        this.qualityGoal = qualityGoal;
        this.goalWeights = goalWeights;
        this.orderedCandidates = List.copyOf(orderedCandidates);
        this.dependencyConstraintCount = dependencyConstraintCount;
        this.warnings = List.copyOf(warnings);
        this.plannedAt = Instant.now().toString();
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public QualityGoal getQualityGoal() {
        return qualityGoal;
    }

    public GoalWeights getGoalWeights() {
        return goalWeights;
    }

    public List<RefactoringCandidate> getOrderedCandidates() {
        return orderedCandidates;
    }

    public int getCandidateCount() {
        return orderedCandidates.size();
    }

    public int getDependencyConstraintCount() {
        return dependencyConstraintCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getPlannedAt() {
        return plannedAt;
    }

    @Override
    public String toString() {
        return "WorkflowPlan{" +
                "goal=" + qualityGoal +
                ", candidates=" + orderedCandidates.size() +
                ", constraints=" + dependencyConstraintCount +
                '}';
    }
}
