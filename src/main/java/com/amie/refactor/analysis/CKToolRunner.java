package com.amie.refactor.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the CK Tool command-line interface against a Java source directory.
 *
 * <p>Responsible only for executing the tool and reporting what happened; it does not
 * parse CSV output and makes no refactoring decisions.</p>
 *
 * <p>Selected tool: <strong>CK Tool 0.7.0</strong>
 * ({@code com.github.mauricioaniche:ck:0.7.0}, executable
 * {@code ck-0.7.0-jar-with-dependencies.jar}), documented command line:</p>
 *
 * <pre>
 *   java -jar ck.jar &lt;projectDir&gt; &lt;useJars:true|false&gt;
 *        &lt;maxFilesPerPartition:0=automatic&gt;
 *        &lt;variablesAndFields:true|false&gt; &lt;outputDir&gt;
 * </pre>
 *
 * <p>Two CK Tool 0.7.0 behaviours this runner respects: (1) output file names are built by
 * string concatenation ({@code outputDir + "class.csv"}), so the output directory argument
 * must end with a path separator; (2) {@code variable.csv}/{@code field.csv} are only
 * produced when variable/field metrics are enabled (this runner disables them).</p>
 */
public class CKToolRunner {

    /** Default CK Tool JAR location, relative to the working directory. */
    public static final String DEFAULT_CK_JAR_RELATIVE_PATH =
            "tools/ck-0.7.0-jar-with-dependencies.jar";

    /** System property that overrides the CK Tool JAR location. */
    public static final String CK_JAR_PROPERTY = "ck.jar";

    /** Environment variable that overrides the CK Tool JAR location. */
    public static final String CK_JAR_ENVIRONMENT_VARIABLE = "CK_JAR";

    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final Path ckJar;
    private final Path outputDirectory;
    private final Duration timeout;
    private final boolean useJars;
    private final ProcessExecutor processExecutor;
    private final String javaExecutable;

    public CKToolRunner(Path ckJar, Path outputDirectory, Duration timeout, boolean useJars) {
        this(ckJar, outputDirectory, timeout, useJars,
                ProcessExecutor.system(), defaultJavaExecutable());
    }

    public CKToolRunner(
            Path ckJar,
            Path outputDirectory,
            Duration timeout,
            boolean useJars,
            ProcessExecutor processExecutor,
            String javaExecutable) {

        this.ckJar = ckJar;
        this.outputDirectory = outputDirectory;
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.useJars = useJars;
        this.processExecutor = processExecutor;
        this.javaExecutable = javaExecutable;
    }

    /**
     * Resolves the CK Tool JAR from (in order) the {@code -Dck.jar} system property, the
     * {@code CK_JAR} environment variable, or the default project-relative location.
     */
    public static Path resolveCkJar() {
        String property = System.getProperty(CK_JAR_PROPERTY);
        if (property != null && !property.isBlank()) {
            return Path.of(property.trim());
        }
        String environment = System.getenv(CK_JAR_ENVIRONMENT_VARIABLE);
        if (environment != null && !environment.isBlank()) {
            return Path.of(environment.trim());
        }
        return Path.of(DEFAULT_CK_JAR_RELATIVE_PATH);
    }

    public Path getCkJar() {
        return ckJar;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Builds the exact command line that will be executed. Exposed so failures can be
     * diagnosed ("what exactly did we run?") and so the command can be unit tested.
     */
    public List<String> buildCommand(Path projectDirectory) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("-jar");
        command.add(ckJar.toString());
        command.add(projectDirectory.toAbsolutePath().normalize().toString());
        command.add(String.valueOf(useJars));
        command.add("0");     // max files per partition: 0 = automatic selection
        command.add("false"); // variable/field metrics: not needed for class metrics
        command.add(outputDirectoryArgument());
        return command;
    }

    /**
     * Runs CK Tool and returns the path of the produced {@code class.csv} file.
     *
     * @throws IllegalArgumentException if {@code projectDirectory} is not an existing directory
     * @throws CKToolException          on any configuration or execution failure
     * @throws IOException              if the process itself cannot be started
     * @throws InterruptedException     if the current thread is interrupted while waiting
     */
    public Path run(Path projectDirectory) throws IOException, InterruptedException {

        if (projectDirectory == null || !Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException(
                    "CK Tool target is not an existing directory: " + projectDirectory);
        }
        if (ckJar == null) {
            throw new CKToolException("CK Tool JAR path is not configured. Set -D"
                    + CK_JAR_PROPERTY + "=... or the " + CK_JAR_ENVIRONMENT_VARIABLE
                    + " environment variable, or place the JAR at " + DEFAULT_CK_JAR_RELATIVE_PATH);
        }
        if (!Files.isRegularFile(ckJar)) {
            throw new CKToolException("CK Tool JAR not found at: " + ckJar.toAbsolutePath()
                    + ". Fetch it from Maven Central with tools/fetch-ck.ps1, or set -D"
                    + CK_JAR_PROPERTY + " / " + CK_JAR_ENVIRONMENT_VARIABLE + ".");
        }
        if (outputDirectory == null) {
            throw new CKToolException("CK Tool output directory is not configured");
        }

        Files.createDirectories(outputDirectory);
        Path absoluteOutput = outputDirectory.toAbsolutePath().normalize();
        Path classCsv = absoluteOutput.resolve("class.csv");

        List<String> command = buildCommand(projectDirectory);

        ProcessExecutor.Result result = processExecutor.execute(command, null, timeout);

        if (result.timedOut()) {
            throw new CKToolException("CK Tool timed out after " + timeout.toSeconds()
                    + "s." + describe(command, result));
        }
        if (result.exitCode() != 0) {
            throw new CKToolException("CK Tool execution failed with exit code "
                    + result.exitCode() + "." + describe(command, result));
        }
        if (!Files.isRegularFile(classCsv)) {
            throw new CKToolException("CK Tool finished successfully but did not produce the "
                    + "expected file: " + classCsv + describe(command, result));
        }
        return classCsv;
    }

    /**
     * Returns the output directory as a command-line argument with a trailing path
     * separator (required by CK Tool 0.7.0's file-name construction).
     */
    private String outputDirectoryArgument() {
        String value = outputDirectory.toAbsolutePath().normalize().toString();
        char last = value.charAt(value.length() - 1);
        if (last != File.separatorChar && last != '/' && last != '\\') {
            value = value + File.separator;
        }
        return value;
    }

    private static String describe(List<String> command, ProcessExecutor.Result result) {
        StringBuilder sink = new StringBuilder();
        sink.append(System.lineSeparator()).append("Command: ").append(String.join(" ", command));
        if (result.standardOutput() != null && !result.standardOutput().isBlank()) {
            sink.append(System.lineSeparator()).append("stdout: ").append(result.standardOutput().trim());
        }
        if (result.standardError() != null && !result.standardError().isBlank()) {
            sink.append(System.lineSeparator()).append("stderr: ").append(result.standardError().trim());
        }
        return sink.toString();
    }

    private static String defaultJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            Path candidate = Path.of(javaHome, "bin", windows ? "java.exe" : "java");
            if (Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        }
        return "java";
    }
}