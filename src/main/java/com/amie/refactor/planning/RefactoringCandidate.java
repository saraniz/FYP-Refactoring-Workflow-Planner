package com.amie.refactor.planning;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.MethodStructure;

import java.util.*;

/**
 * Normalized model for a prioritized refactoring candidate.
 * Connects code smell findings, CK metrics, AST structures, and planning scores.
 */
public class RefactoringCandidate implements Comparable<RefactoringCandidate> {

    private final String candidateId;
    private final CodeSmell originalSmell;
    private final SmellType smellType;
    private final RefactoringAction recommendedAction;

    private final String fullyQualifiedClassName;
    private final String className;
    private final String methodName;
    private final String filePath;
    private final int startLine;
    private final int endLine;

    private final ClassMetrics classMetrics;
    private final MethodStructure methodStructure;
    private final ClassStructure classStructure;

    private double priorityScore;
    private ScoreBreakdown scoreBreakdown;
    private final Set<String> prerequisiteIds;
    private com.amie.refactor.diff.RefactoringDiff diffPreview;
    private com.amie.refactor.forecast.MetricForecast metricForecast;
    private com.amie.refactor.forecast.BlastRadiusInfo blastRadius;

    public RefactoringCandidate(
            String candidateId,
            CodeSmell originalSmell,
            RefactoringAction recommendedAction,
            ClassMetrics classMetrics,
            MethodStructure methodStructure,
            ClassStructure classStructure) {

        this.candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null");
        this.originalSmell = Objects.requireNonNull(originalSmell, "originalSmell must not be null");
        this.smellType = originalSmell.getSmellType();
        this.recommendedAction = recommendedAction;

        this.fullyQualifiedClassName = originalSmell.getFullyQualifiedClassName() != null
                ? originalSmell.getFullyQualifiedClassName()
                : originalSmell.getClassName();
        this.className = originalSmell.getClassName();
        this.methodName = originalSmell.getMethodName();
        this.filePath = originalSmell.getFilePath();
        this.startLine = originalSmell.getStartLine();
        this.endLine = originalSmell.getEndLine();

        this.classMetrics = classMetrics;
        this.methodStructure = methodStructure;
        this.classStructure = classStructure;

        this.priorityScore = 0.0;
        this.scoreBreakdown = null;
        this.prerequisiteIds = new LinkedHashSet<>();
    }

    public String getCandidateId() {
        return candidateId;
    }

    public CodeSmell getOriginalSmell() {
        return originalSmell;
    }

    public SmellType getSmellType() {
        return smellType;
    }

    public RefactoringAction getRecommendedAction() {
        return recommendedAction;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public ClassMetrics getClassMetrics() {
        return classMetrics;
    }

    public MethodStructure getMethodStructure() {
        return methodStructure;
    }

    public ClassStructure getClassStructure() {
        return classStructure;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public ScoreBreakdown getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
        if (scoreBreakdown != null) {
            this.priorityScore = scoreBreakdown.getFinalPriorityScore();
        }
    }

    public Set<String> getPrerequisiteIds() {
        return Collections.unmodifiableSet(prerequisiteIds);
    }

    public void addPrerequisite(String prerequisiteCandidateId) {
        if (!candidateId.equals(prerequisiteCandidateId)) {
            this.prerequisiteIds.add(prerequisiteCandidateId);
        }
    }

    public void removePrerequisite(String prerequisiteCandidateId) {
        this.prerequisiteIds.remove(prerequisiteCandidateId);
    }

    public com.amie.refactor.diff.RefactoringDiff getDiffPreview() {
        return diffPreview;
    }

    public void setDiffPreview(com.amie.refactor.diff.RefactoringDiff diffPreview) {
        this.diffPreview = diffPreview;
    }

    public com.amie.refactor.forecast.MetricForecast getMetricForecast() {
        return metricForecast;
    }

    public void setMetricForecast(com.amie.refactor.forecast.MetricForecast metricForecast) {
        this.metricForecast = metricForecast;
    }

    public com.amie.refactor.forecast.BlastRadiusInfo getBlastRadius() {
        return blastRadius;
    }

    public void setBlastRadius(com.amie.refactor.forecast.BlastRadiusInfo blastRadius) {
        this.blastRadius = blastRadius;
    }

    @Override
    public int compareTo(RefactoringCandidate o) {
        // Descending order of priority score
        int cmp = Double.compare(o.priorityScore, this.priorityScore);
        if (cmp != 0) return cmp;
        return this.candidateId.compareTo(o.candidateId);
    }

    @Override
    public String toString() {
        return candidateId + ": " + recommendedAction.getActionName() + " on "
                + className + (methodName != null ? "." + methodName : "")
                + " [Score=" + String.format("%.3f", priorityScore) + "]";
    }
}
