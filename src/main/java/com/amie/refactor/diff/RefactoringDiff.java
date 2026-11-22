package com.amie.refactor.diff;

import com.amie.refactor.planning.RefactoringAction;

import java.util.Objects;

/**
 * Model holding the before-and-after source code diff preview
 * and unified patch for a prioritized refactoring recommendation.
 */
public class RefactoringDiff {

    private final String candidateId;
    private final String targetElement;
    private final RefactoringAction action;
    private final String filePath;
    private final String beforeCode;
    private final String afterCode;
    private final String unifiedDiff;
    private final String explanationSummary;

    public RefactoringDiff(
            String candidateId,
            String targetElement,
            RefactoringAction action,
            String filePath,
            String beforeCode,
            String afterCode,
            String unifiedDiff,
            String explanationSummary) {

        this.candidateId = Objects.requireNonNull(candidateId, "candidateId must not be null");
        this.targetElement = Objects.requireNonNull(targetElement, "targetElement must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.filePath = filePath != null ? filePath : "";
        this.beforeCode = Objects.requireNonNull(beforeCode, "beforeCode must not be null");
        this.afterCode = Objects.requireNonNull(afterCode, "afterCode must not be null");
        this.unifiedDiff = Objects.requireNonNull(unifiedDiff, "unifiedDiff must not be null");
        this.explanationSummary = explanationSummary != null ? explanationSummary : "";
    }

    public String getCandidateId() {
        return candidateId;
    }

    public String getTargetElement() {
        return targetElement;
    }

    public RefactoringAction getAction() {
        return action;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBeforeCode() {
        return beforeCode;
    }

    public String getAfterCode() {
        return afterCode;
    }

    public String getUnifiedDiff() {
        return unifiedDiff;
    }

    public String getExplanationSummary() {
        return explanationSummary;
    }

    @Override
    public String toString() {
        return "RefactoringDiff{" +
                "candidateId='" + candidateId + '\'' +
                ", targetElement='" + targetElement + '\'' +
                ", action=" + action.getActionName() +
                '}';
    }
}
