package com.amie.refactor.model.dependency;

import java.util.Objects;

/**
 * Represents a single directed dependency from a source Java artifact to a target artifact.
 */
public class Dependency {

    private final String sourceClass;
    private final String sourceMethod;
    private final String targetClass;
    private final String targetMethod;
    private final DependencyType type;
    private final int lineNumber;

    public Dependency(
            String sourceClass,
            String sourceMethod,
            String targetClass,
            String targetMethod,
            DependencyType type,
            int lineNumber) {

        this.sourceClass = Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        this.sourceMethod = sourceMethod;
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass must not be null");
        this.targetMethod = targetMethod;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.lineNumber = lineNumber;
    }

    public String getSourceClass() {
        return sourceClass;
    }

    public String getSourceMethod() {
        return sourceMethod;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public DependencyType getType() {
        return type;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency that)) return false;
        return lineNumber == that.lineNumber &&
                Objects.equals(sourceClass, that.sourceClass) &&
                Objects.equals(sourceMethod, that.sourceMethod) &&
                Objects.equals(targetClass, that.targetClass) &&
                Objects.equals(targetMethod, that.targetMethod) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceClass, sourceMethod, targetClass, targetMethod, type, lineNumber);
    }

    @Override
    public String toString() {
        return sourceClass + (sourceMethod != null ? "." + sourceMethod : "")
                + " -> " + type + " -> "
                + targetClass + (targetMethod != null ? "." + targetMethod : "")
                + " (line " + lineNumber + ")";
    }
}
