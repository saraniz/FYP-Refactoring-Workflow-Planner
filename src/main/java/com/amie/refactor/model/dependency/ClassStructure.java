package com.amie.refactor.model.dependency;

import java.util.*;

/**
 * Structural details of a class extracted via JavaParser AST analysis,
 * including its methods, fields, and computed coupling / dependency links.
 */
public class ClassStructure {

    private final String fullyQualifiedClassName;
    private final String simpleName;
    private final String packageName;
    private final String filePath;
    private final boolean isInterface;
    private final String superClassName;
    private final List<String> implementedInterfaces;
    private final Map<String, String> fields; // fieldName -> fieldType
    private final List<MethodStructure> methods;
    private final List<Dependency> outgoingDependencies;
    private final List<Dependency> incomingDependencies;
    private int fanIn;
    private int fanOut;

    public ClassStructure(
            String fullyQualifiedClassName,
            String simpleName,
            String packageName,
            String filePath,
            boolean isInterface,
            String superClassName,
            List<String> implementedInterfaces,
            Map<String, String> fields,
            List<MethodStructure> methods) {

        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.simpleName = simpleName;
        this.packageName = packageName != null ? packageName : "";
        this.filePath = filePath;
        this.isInterface = isInterface;
        this.superClassName = superClassName;
        this.implementedInterfaces = List.copyOf(implementedInterfaces);
        this.fields = Map.copyOf(fields);
        this.methods = List.copyOf(methods);
        this.outgoingDependencies = new ArrayList<>();
        this.incomingDependencies = new ArrayList<>();
        this.fanIn = 0;
        this.fanOut = 0;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public List<MethodStructure> getMethods() {
        return methods;
    }

    public Optional<MethodStructure> findMethod(String methodName) {
        if (methodName == null) return Optional.empty();
        return methods.stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst();
    }

    public List<Dependency> getOutgoingDependencies() {
        return Collections.unmodifiableList(outgoingDependencies);
    }

    public List<Dependency> getIncomingDependencies() {
        return Collections.unmodifiableList(incomingDependencies);
    }

    public void addOutgoingDependency(Dependency dependency) {
        this.outgoingDependencies.add(dependency);
    }

    public void addIncomingDependency(Dependency dependency) {
        this.incomingDependencies.add(dependency);
    }

    public int getFanIn() {
        return fanIn;
    }

    public void setFanIn(int fanIn) {
        this.fanIn = fanIn;
    }

    public int getFanOut() {
        return fanOut;
    }

    public void setFanOut(int fanOut) {
        this.fanOut = fanOut;
    }

    /**
     * Set of unique target class names this class depends upon.
     */
    public Set<String> getDependentClassNames() {
        Set<String> set = new HashSet<>();
        for (Dependency d : outgoingDependencies) {
            set.add(d.getTargetClass());
        }
        return set;
    }

    /**
     * Set of unique source class names that depend upon this class.
     */
    public Set<String> getCallerClassNames() {
        Set<String> set = new HashSet<>();
        for (Dependency d : incomingDependencies) {
            set.add(d.getSourceClass());
        }
        return set;
    }

    @Override
    public String toString() {
        return fullyQualifiedClassName + " [Methods=" + methods.size()
                + ", FanIn=" + fanIn + ", FanOut=" + fanOut + "]";
    }
}
