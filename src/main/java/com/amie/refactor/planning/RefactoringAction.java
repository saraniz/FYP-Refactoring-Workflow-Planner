package com.amie.refactor.planning;

/**
 * Recommended Fowler (2018) refactoring techniques mapped to detected code smells.
 */
public enum RefactoringAction {

    /** Recommended for Long Method: extracts cohesive block into a helper method. */
    EXTRACT_METHOD("Extract Method", "Extract cohesive statements into a new well-named method to reduce method length and complexity."),

    /** Recommended for God Class: breaks oversized class into smaller cohesive classes. */
    EXTRACT_CLASS("Extract Class", "Extract a subset of closely related responsibilities and fields into a separate cohesive class."),

    /** Recommended for Feature Envy: moves envy method to the class it uses most. */
    MOVE_METHOD("Move Method", "Move the method to the target class whose data and behavior it accesses most frequently."),

    /** Recommended for Data Clumps: bundles recurring parameters/fields into an object. */
    INTRODUCE_PARAMETER_OBJECT("Introduce Parameter Object", "Group repeatedly co-occurring parameters into a dedicated value or parameter class.");

    private final String actionName;
    private final String description;

    RefactoringAction(String actionName, String description) {
        this.actionName = actionName;
        this.description = description;
    }

    public String getActionName() {
        return actionName;
    }

    public String getDescription() {
        return description;
    }
}
