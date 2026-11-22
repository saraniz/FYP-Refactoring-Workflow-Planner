package com.amie.refactor.model;

// this create the java object that contain the details about each code smell..that mean each code smell should contain these details
public class CodeSmell {

    private final SmellType smellType;
    private final String className;

    // Fully qualified class name (package + class name),
    // used to associate PMD findings with CK Tool
    // metrics. PMD reports the package and the class
    // separately in its XML output. It is null only
    // when the smell was created through the legacy
    // constructor below.
    private final String fullyQualifiedClassName;
    private final String methodName;
    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final String severity;
    private final String ruleName;
    private final String message;

    // Legacy constructor: keeps the simple class name
    // only. Prefer the constructor below.
    public CodeSmell(
            SmellType smellType,
            String className,
            String methodName,
            String filePath,
            int startLine,
            int endLine,
            String severity,
            String ruleName,
            String message) {

        this(smellType, className, null, methodName, filePath,
                startLine, endLine, severity, ruleName, message);
    }

    public CodeSmell(
            SmellType smellType,
            String className,
            String fullyQualifiedClassName,
            String methodName,
            String filePath,
            int startLine,
            int endLine,
            String severity,
            String ruleName,
            String message) {

        this.smellType = smellType;
        this.className = className;
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.methodName = methodName;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.severity = severity;
        this.ruleName = ruleName;
        this.message = message;
    }

    public SmellType getSmellType() {
        return smellType;
    }

    public String getClassName() {
        return className;
    }

    // Fully qualified class name (e.g. "demo.OrderService"),
    // or null when only the simple name is known.
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
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

    public String getSeverity() {
        return severity;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getMessage() {
        return message;
    }
}