package com.amie.refactor.explanation;

import com.amie.refactor.planning.RefactoringCandidate;

import java.util.Objects;

/**
 * An individual step within the prioritized refactoring workflow,
 * combining the scheduled candidate with its code-aware explanation.
 */
public class WorkflowStep {

    private final int stepNumber;
    private final RefactoringCandidate candidate;
    private final String title;
    private final StepExplanation explanation;

    public WorkflowStep(
            int stepNumber,
            RefactoringCandidate candidate,
            String title,
            StepExplanation explanation) {

        this.stepNumber = stepNumber;
        this.candidate = Objects.requireNonNull(candidate, "candidate must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.explanation = Objects.requireNonNull(explanation, "explanation must not be null");
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public RefactoringCandidate getCandidate() {
        return candidate;
    }

    public String getTitle() {
        return title;
    }

    public StepExplanation getExplanation() {
        return explanation;
    }

    public com.amie.refactor.diff.RefactoringDiff getDiffPreview() {
        return candidate.getDiffPreview();
    }

    public com.amie.refactor.forecast.MetricForecast getMetricForecast() {
        return candidate.getMetricForecast();
    }

    public com.amie.refactor.forecast.BlastRadiusInfo getBlastRadius() {
        return candidate.getBlastRadius();
    }

    @Override
    public String toString() {
        return "Step " + stepNumber + ": " + title + " (Score: "
                + String.format("%.3f", candidate.getPriorityScore()) + ")";
    }
}
