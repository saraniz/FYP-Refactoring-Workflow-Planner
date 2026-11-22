package com.amie.refactor.analysis.dependency;

import com.amie.refactor.analysis.ProjectScanner;
import com.amie.refactor.model.dependency.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Source code and dependency analyzer built on JavaParser AST.
 * Implements Objective 2 (Part B) of the thesis proposal.
 *
 * <p>Extracts classes, methods, cyclomatic complexity, field references, method calls,
 * and builds fine-grained directed dependency links with fan-in and fan-out calculation.</p>
 */
public class JavaParserDependencyAnalyzer {

    private final JavaParser parser;

    public JavaParserDependencyAnalyzer() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.parser = new JavaParser(config);
    }

    public DependencyAnalysisResult analyze(Path targetDirectory) {
        Objects.requireNonNull(targetDirectory, "targetDirectory must not be null");

        List<String> warnings = new ArrayList<>();
        Map<String, ClassStructure> classMap = new LinkedHashMap<>();
        List<Dependency> rawDependencies = new ArrayList<>();

        if (!Files.exists(targetDirectory)) {
            warnings.add("Target directory does not exist: " + targetDirectory.toAbsolutePath());
            return new DependencyAnalysisResult(targetDirectory.toString(), classMap, rawDependencies, warnings);
        }

        List<Path> javaFiles;
        try {
            javaFiles = new ProjectScanner().findJavaFiles(targetDirectory);
        } catch (Exception e) {
            warnings.add("Failed to scan Java files in " + targetDirectory + ": " + e.getMessage());
            return new DependencyAnalysisResult(targetDirectory.toString(), classMap, rawDependencies, warnings);
        }
        if (javaFiles.isEmpty()) {
            warnings.add("No Java source files found in: " + targetDirectory.toAbsolutePath());
            return new DependencyAnalysisResult(targetDirectory.toString(), classMap, rawDependencies, warnings);
        }

        // Map for simple name to FQN resolution across this project
        Map<String, String> simpleToFqn = new HashMap<>();

        // Phase 1: Parse all compilation units and build ClassStructures
        List<ParsedFile> parsedFiles = new ArrayList<>();
        for (Path file : javaFiles) {
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (!result.isSuccessful() || result.getResult().isEmpty()) {
                    warnings.add("Failed to parse " + file + ": " + result.getProblems());
                    continue;
                }
                CompilationUnit cu = result.getResult().get();
                parsedFiles.add(new ParsedFile(file, cu));

                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getName().asString())
                        .orElse("");

                for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                    String simpleName = cid.getNameAsString();
                    String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
                    simpleToFqn.put(simpleName, fqn);
                    simpleToFqn.put(fqn, fqn);
                }
            } catch (IOException e) {
                warnings.add("I/O error reading " + file + ": " + e.getMessage());
            }
        }

        // Phase 2: Extract structural details & raw dependencies from each AST
        for (ParsedFile pf : parsedFiles) {
            CompilationUnit cu = pf.cu;
            Path filePath = pf.path;

            String packageName = cu.getPackageDeclaration()
                    .map(pd -> pd.getName().asString())
                    .orElse("");

            // Track imports in this file
            Map<String, String> fileImports = new HashMap<>();
            for (ImportDeclaration imp : cu.getImports()) {
                String importStr = imp.getNameAsString();
                String simpleImport = importStr.substring(importStr.lastIndexOf('.') + 1);
                fileImports.put(simpleImport, importStr);
            }

            for (ClassOrInterfaceDeclaration cid : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String simpleName = cid.getNameAsString();
                String fqn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
                boolean isInterface = cid.isInterface();

                String superClassName = cid.getExtendedTypes().isNonEmpty()
                        ? cid.getExtendedTypes(0).getNameAsString()
                        : null;

                List<String> implemented = new ArrayList<>();
                cid.getImplementedTypes().forEach(t -> implemented.add(t.getNameAsString()));

                // Inheritance dependencies
                if (superClassName != null) {
                    rawDependencies.add(new Dependency(
                            fqn, null, resolveType(superClassName, packageName, fileImports, simpleToFqn),
                            null, DependencyType.EXTENDS,
                            cid.getBegin().map(p -> p.line).orElse(1)));
                }
                for (String iface : implemented) {
                    rawDependencies.add(new Dependency(
                            fqn, null, resolveType(iface, packageName, fileImports, simpleToFqn),
                            null, DependencyType.IMPLEMENTS,
                            cid.getBegin().map(p -> p.line).orElse(1)));
                }

                // Fields
                Map<String, String> fields = new LinkedHashMap<>();
                for (FieldDeclaration fd : cid.getFields()) {
                    String fieldType = fd.getElementType().asString();
                    for (VariableDeclarator vd : fd.getVariables()) {
                        String fieldName = vd.getNameAsString();
                        fields.put(fieldName, fieldType);

                        int line = vd.getBegin().map(p -> p.line).orElse(1);
                        String resolvedType = resolveType(fieldType, packageName, fileImports, simpleToFqn);
                        rawDependencies.add(new Dependency(
                                fqn, null, resolvedType, fieldName, DependencyType.CONTAINS, line));

                        if (vd.getInitializer().isPresent() && vd.getInitializer().get().isObjectCreationExpr()) {
                            rawDependencies.add(new Dependency(
                                    fqn, null, resolvedType, null, DependencyType.CREATES, line));
                        }
                    }
                }

                // Methods
                List<MethodStructure> methodStructures = new ArrayList<>();
                for (MethodDeclaration md : cid.getMethods()) {
                    String methodName = md.getNameAsString();
                    String returnType = md.getTypeAsString();
                    int startLine = md.getBegin().map(p -> p.line).orElse(1);
                    int endLine = md.getEnd().map(p -> p.line).orElse(startLine);
                    int lineCount = endLine - startLine + 1;

                    List<String> paramTypes = new ArrayList<>();
                    List<String> paramNames = new ArrayList<>();
                    for (Parameter param : md.getParameters()) {
                        paramTypes.add(param.getTypeAsString());
                        paramNames.add(param.getNameAsString());

                        String resolvedParamType = resolveType(param.getTypeAsString(), packageName, fileImports, simpleToFqn);
                        rawDependencies.add(new Dependency(
                                fqn, methodName, resolvedParamType, param.getNameAsString(),
                                DependencyType.PARAMETER, param.getBegin().map(p -> p.line).orElse(startLine)));
                    }

                    if (!"void".equals(returnType)) {
                        String resolvedReturn = resolveType(returnType, packageName, fileImports, simpleToFqn);
                        rawDependencies.add(new Dependency(
                                fqn, methodName, resolvedReturn, null,
                                DependencyType.RETURNS, startLine));
                    }

                    // Cyclomatic Complexity calculation
                    int cc = computeCyclomaticComplexity(md);

                    // Method calls & field accesses inside this method
                    List<String> calledMethods = new ArrayList<>();
                    List<String> accessedFields = new ArrayList<>();

                    md.findAll(MethodCallExpr.class).forEach(mce -> {
                        String calledMethodName = mce.getNameAsString();
                        calledMethods.add(calledMethodName);
                        int callLine = mce.getBegin().map(p -> p.line).orElse(startLine);

                        String targetClass = fqn; // default self
                        if (mce.getScope().isPresent()) {
                            Expression scope = mce.getScope().get();
                            String scopeStr = scope.toString();
                            if (fields.containsKey(scopeStr)) {
                                targetClass = resolveType(fields.get(scopeStr), packageName, fileImports, simpleToFqn);
                            } else if (simpleToFqn.containsKey(scopeStr)) {
                                targetClass = simpleToFqn.get(scopeStr);
                            } else if (fileImports.containsKey(scopeStr)) {
                                targetClass = fileImports.get(scopeStr);
                            } else {
                                targetClass = scopeStr;
                            }
                        }

                        rawDependencies.add(new Dependency(
                                fqn, methodName, targetClass, calledMethodName,
                                DependencyType.CALLS, callLine));
                    });

                    md.findAll(FieldAccessExpr.class).forEach(fae -> {
                        String fieldName = fae.getNameAsString();
                        accessedFields.add(fieldName);
                        int accessLine = fae.getBegin().map(p -> p.line).orElse(startLine);
                        String scopeStr = fae.getScope().toString();

                        String targetClass = fqn;
                        if (fields.containsKey(scopeStr)) {
                            targetClass = resolveType(fields.get(scopeStr), packageName, fileImports, simpleToFqn);
                        } else if (simpleToFqn.containsKey(scopeStr)) {
                            targetClass = simpleToFqn.get(scopeStr);
                        }

                        rawDependencies.add(new Dependency(
                                fqn, methodName, targetClass, fieldName,
                                DependencyType.USES_FIELD, accessLine));
                    });

                    methodStructures.add(new MethodStructure(
                            fqn, methodName, returnType, paramTypes, paramNames,
                            startLine, endLine, lineCount, cc, calledMethods, accessedFields));
                }

                ClassStructure classStructure = new ClassStructure(
                        fqn, simpleName, packageName, filePath.toString(),
                        isInterface, superClassName, implemented, fields, methodStructures);

                classMap.put(fqn, classStructure);
            }
        }

        // Phase 3: Wire outgoing/incoming dependencies and compute fan-in / fan-out
        for (Dependency dep : rawDependencies) {
            String src = dep.getSourceClass();
            String tgt = dep.getTargetClass();

            ClassStructure srcStruct = classMap.get(src);
            if (srcStruct != null && !src.equals(tgt)) {
                srcStruct.addOutgoingDependency(dep);
            }

            ClassStructure tgtStruct = classMap.get(tgt);
            if (tgtStruct != null && !src.equals(tgt)) {
                tgtStruct.addIncomingDependency(dep);
            }
        }

        for (ClassStructure cs : classMap.values()) {
            // Fan-out: unique project classes this class depends upon
            Set<String> distinctOutgoing = new HashSet<>();
            for (Dependency d : cs.getOutgoingDependencies()) {
                if (classMap.containsKey(d.getTargetClass()) && !d.getTargetClass().equals(cs.getFullyQualifiedClassName())) {
                    distinctOutgoing.add(d.getTargetClass());
                }
            }
            cs.setFanOut(distinctOutgoing.size());

            // Fan-in: unique project classes that depend upon this class
            Set<String> distinctIncoming = new HashSet<>();
            for (Dependency d : cs.getIncomingDependencies()) {
                if (classMap.containsKey(d.getSourceClass()) && !d.getSourceClass().equals(cs.getFullyQualifiedClassName())) {
                    distinctIncoming.add(d.getSourceClass());
                }
            }
            cs.setFanIn(distinctIncoming.size());
        }

        return new DependencyAnalysisResult(
                targetDirectory.toString(),
                classMap,
                rawDependencies,
                warnings);
    }

    private static String resolveType(
            String rawType,
            String currentPackage,
            Map<String, String> fileImports,
            Map<String, String> simpleToFqn) {

        if (rawType == null) return "unknown";

        // Strip generic arguments, array brackets
        String cleanType = rawType.replaceAll("<.*>", "").replaceAll("\\[\\]", "").trim();

        if (fileImports.containsKey(cleanType)) {
            return fileImports.get(cleanType);
        }

        if (simpleToFqn.containsKey(cleanType)) {
            return simpleToFqn.get(cleanType);
        }

        if (!currentPackage.isEmpty() && simpleToFqn.containsKey(currentPackage + "." + cleanType)) {
            return currentPackage + "." + cleanType;
        }

        return cleanType;
    }

    /**
     * Computes McCabe cyclomatic complexity of a method:
     * Base = 1
     * +1 for each conditional / branch / loop / catch / ternary.
     */
    private int computeCyclomaticComplexity(MethodDeclaration md) {
        int complexity = 1;
        for (Node node : md.findAll(Node.class)) {
            if (node instanceof IfStmt ||
                    node instanceof ForStmt ||
                    node instanceof ForEachStmt ||
                    node instanceof WhileStmt ||
                    node instanceof DoStmt ||
                    node instanceof CatchClause ||
                    node instanceof ConditionalExpr) {
                complexity++;
            } else if (node instanceof SwitchEntry se && se.getLabels().isNonEmpty()) {
                complexity++;
            } else if (node instanceof BinaryExpr be) {
                BinaryExpr.Operator op = be.getOperator();
                if (op == BinaryExpr.Operator.AND || op == BinaryExpr.Operator.OR) {
                    complexity++;
                }
            }
        }
        return complexity;
    }

    private static class ParsedFile {
        final Path path;
        final CompilationUnit cu;

        ParsedFile(Path path, CompilationUnit cu) {
            this.path = path;
            this.cu = cu;
        }
    }
}
