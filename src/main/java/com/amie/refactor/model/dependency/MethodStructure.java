package com.amie.refactor.model.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structural details of a method extracted via JavaParser AST analysis.
 */
public class MethodStructure {

    private final String className;
    private final String methodName;
    private final String returnType;
    private final List<String> parameterTypes;
    private final List<String> parameterNames;
    private final int startLine;
    private final int endLine;
    private final int lineCount;
    private final int cyclomaticComplexity;
    private final List<String> calledMethods;
    private final List<String> accessedFields;

    public MethodStructure(
            String className,
            String methodName,
            String returnType,
            List<String> parameterTypes,
            List<String> parameterNames,
            int startLine,
            int endLine,
            int lineCount,
            int cyclomaticComplexity,
            List<String> calledMethods,
            List<String> accessedFields) {

        this.className = className;
        this.methodName = methodName;
        this.returnType = returnType != null ? returnType : "void";
        this.parameterTypes = List.copyOf(parameterTypes);
        this.parameterNames = List.copyOf(parameterNames);
        this.startLine = startLine;
        this.endLine = endLine;
        this.lineCount = lineCount;
        this.cyclomaticComplexity = cyclomaticComplexity;
        this.calledMethods = List.copyOf(calledMethods);
        this.accessedFields = List.copyOf(accessedFields);
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public int getParameterCount() {
        return parameterTypes.size();
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getLineCount() {
        return lineCount;
    }

    public int getCyclomaticComplexity() {
        return cyclomaticComplexity;
    }

    public List<String> getCalledMethods() {
        return calledMethods;
    }

    public List<String> getAccessedFields() {
        return accessedFields;
    }

    @Override
    public String toString() {
        return returnType + " " + methodName + "(" + String.join(", ", parameterTypes) + ")"
                + " [lines " + startLine + "-" + endLine + ", CC=" + cyclomaticComplexity + "]";
    }
}
