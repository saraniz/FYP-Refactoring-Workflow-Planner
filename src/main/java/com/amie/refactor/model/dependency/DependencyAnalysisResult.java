package com.amie.refactor.model.dependency;

import java.util.*;

/**
 * Result of AST and dependency analysis across all Java files in a target project.
 */
public class DependencyAnalysisResult {

    private final String targetDirectory;
    private final Map<String, ClassStructure> classes; // fullyQualifiedClassName -> ClassStructure
    private final List<Dependency> allDependencies;
    private final List<String> warnings;

    public DependencyAnalysisResult(
            String targetDirectory,
            Map<String, ClassStructure> classes,
            List<Dependency> allDependencies,
            List<String> warnings) {

        this.targetDirectory = targetDirectory;
        this.classes = Map.copyOf(classes);
        this.allDependencies = List.copyOf(allDependencies);
        this.warnings = List.copyOf(warnings);
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public Map<String, ClassStructure> getClasses() {
        return classes;
    }

    public ClassStructure getClassStructure(String className) {
        if (className == null) return null;
        ClassStructure cs = classes.get(className);
        if (cs != null) return cs;

        // Try lookup by simple name if fully qualified wasn't found
        for (ClassStructure item : classes.values()) {
            if (item.getSimpleName().equals(className)) {
                return item;
            }
        }
        return null;
    }

    public List<Dependency> getAllDependencies() {
        return allDependencies;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Checks if classA directly depends on classB (via call, field, extends, implements, creates, etc.).
     */
    public boolean hasDirectDependency(String classA, String classB) {
        ClassStructure csA = getClassStructure(classA);
        if (csA == null || classB == null) return false;

        ClassStructure csB = getClassStructure(classB);
        String targetFqn = csB != null ? csB.getFullyQualifiedClassName() : classB;
        String targetSimple = csB != null ? csB.getSimpleName() : classB;

        for (Dependency d : csA.getOutgoingDependencies()) {
            if (d.getTargetClass().equals(targetFqn) || d.getTargetClass().equals(targetSimple)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all dependencies between classA and classB.
     */
    public List<Dependency> getDependenciesBetween(String classA, String classB) {
        List<Dependency> result = new ArrayList<>();
        ClassStructure csA = getClassStructure(classA);
        if (csA == null || classB == null) return result;

        ClassStructure csB = getClassStructure(classB);
        String targetFqn = csB != null ? csB.getFullyQualifiedClassName() : classB;
        String targetSimple = csB != null ? csB.getSimpleName() : classB;

        for (Dependency d : csA.getOutgoingDependencies()) {
            if (d.getTargetClass().equals(targetFqn) || d.getTargetClass().equals(targetSimple)) {
                result.add(d);
            }
        }
        return result;
    }

    public int getFanIn(String className) {
        ClassStructure cs = getClassStructure(className);
        return cs != null ? cs.getFanIn() : 0;
    }

    public int getFanOut(String className) {
        ClassStructure cs = getClassStructure(className);
        return cs != null ? cs.getFanOut() : 0;
    }

    @Override
    public String toString() {
        return "DependencyAnalysisResult{" +
                "classes=" + classes.size() +
                ", dependencies=" + allDependencies.size() +
                ", warnings=" + warnings.size() +
                '}';
    }
}
