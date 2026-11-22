package com.amie.refactor.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CKToolRunner}. CK Tool is never executed: a fake
 * {@link ProcessExecutor} records the command and simulates exit codes, timeouts and
 * produced files. A placeholder JAR file is used so configuration checks pass.
 */
class CKToolRunnerTest {

    @TempDir
    Path tempDir;

    private Path placeholderJar() throws IOException {
        Path jar = tempDir.resolve("ck-0.7.0-jar-with-dependencies.jar");
        Files.writeString(jar, "placeholder");
        return jar;
    }

    private Path projectDirectory() throws IOException {
        Path project = tempDir.resolve("project");
        Files.createDirectories(project);
        Files.writeString(project.resolve("Demo.java"), "class Demo {}");
        return project;
    }

    @Test
    void buildsCommandWithDocumentedCkToolArgumentOrder() throws Exception {
        Path jar = placeholderJar();
        Path output = tempDir.resolve("out");
        Path project = projectDirectory();

        CKToolRunner runner = new CKToolRunner(
                jar, output, Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(0, "ok", "", false),
                "java");

        List<String> command = runner.buildCommand(project);

        assertEquals("java", command.get(0));
        assertEquals("-jar", command.get(1));
        assertEquals(jar.toString(), command.get(2));
        assertEquals(project.toAbsolutePath().normalize().toString(), command.get(3));
        assertEquals("false", command.get(4)); // useJars
        assertEquals("0", command.get(5));     // max files per partition
        assertEquals("false", command.get(6)); // variables and fields
        assertTrue(command.get(7).endsWith(File.separator),
                "CK 0.7.0 requires the output directory argument to end with a separator");
        assertEquals(8, command.size());
    }

    @Test
    void reportsProcessFailureWithExitCodeAndOutput() throws Exception {
        Path jar = placeholderJar();
        CKToolRunner runner = new CKToolRunner(
                jar, tempDir.resolve("out"), Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(2, "", "boom", false),
                "java");

        CKToolException exception = assertThrows(CKToolException.class,
                () -> runner.run(projectDirectory()));
        assertTrue(exception.getMessage().contains("exit code 2"));
        assertTrue(exception.getMessage().contains("boom"));
    }

    @Test
    void reportsTimeout() throws Exception {
        Path jar = placeholderJar();
        CKToolRunner runner = new CKToolRunner(
                jar, tempDir.resolve("out"), Duration.ofSeconds(1), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(-1, "", "", true),
                "java");

        CKToolException exception = assertThrows(CKToolException.class,
                () -> runner.run(projectDirectory()));
        assertTrue(exception.getMessage().contains("timed out"));
    }

    @Test
    void reportsMissingExpectedOutputFile() throws Exception {
        Path jar = placeholderJar();
        CKToolRunner runner = new CKToolRunner(
                jar, tempDir.resolve("out"), Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(0, "Metrics extracted!!!", "", false),
                "java");

        CKToolException exception = assertThrows(CKToolException.class,
                () -> runner.run(projectDirectory()));
        assertTrue(exception.getMessage().contains("did not produce"));
    }

    @Test
    void returnsClassCsvPathWhenToolSucceeds() throws Exception {
        Path jar = placeholderJar();
        Path output = tempDir.resolve("out");
        CKToolRunner runner = new CKToolRunner(
                jar, output, Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) -> {
                    // Simulate CK writing class.csv into the configured output directory.
                    Path outDir = Path.of(command.get(command.size() - 1));
                    Files.createDirectories(outDir);
                    Files.writeString(outDir.resolve("class.csv"),
                            "class,cbo,wmc,lcom\nA,1,2,3\n");
                    return new ProcessExecutor.Result(0, "Metrics extracted!!!", "", false);
                },
                "java");

        Path classCsv = runner.run(projectDirectory());

        assertEquals(output.toAbsolutePath().normalize().resolve("class.csv"), classCsv);
        assertTrue(Files.isRegularFile(classCsv));
    }

    @Test
    void failsCleanlyWhenJarIsMissing() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean(false);
        CKToolRunner runner = new CKToolRunner(
                tempDir.resolve("absent.jar"), tempDir.resolve("out"), Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) -> {
                    invoked.set(true);
                    return new ProcessExecutor.Result(0, "", "", false);
                },
                "java");

        CKToolException exception = assertThrows(CKToolException.class,
                () -> runner.run(projectDirectory()));
        assertTrue(exception.getMessage().contains("CK Tool JAR not found"));
        assertFalse(invoked.get(), "the tool must not be executed when the JAR is missing");
    }

    @Test
    void failsCleanlyForInvalidTargetDirectory() throws Exception {
        CKToolRunner runner = new CKToolRunner(
                placeholderJar(), tempDir.resolve("out"), Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(0, "", "", false),
                "java");

        assertThrows(IllegalArgumentException.class,
                () -> runner.run(tempDir.resolve("no-such-dir")));
    }

    @Test
    void resolvesJarFromSystemPropertyWhenSet() {
        String previous = System.getProperty(CKToolRunner.CK_JAR_PROPERTY);
        try {
            System.setProperty(CKToolRunner.CK_JAR_PROPERTY, "D:/custom/ck.jar");
            assertEquals(Path.of("D:/custom/ck.jar"), CKToolRunner.resolveCkJar());
        } finally {
            if (previous == null) {
                System.clearProperty(CKToolRunner.CK_JAR_PROPERTY);
            } else {
                System.setProperty(CKToolRunner.CK_JAR_PROPERTY, previous);
            }
        }
    }

    @Test
    void fallsBackToDefaultJarLocationWhenNotConfigured() {
        String previous = System.getProperty(CKToolRunner.CK_JAR_PROPERTY);
        try {
            System.clearProperty(CKToolRunner.CK_JAR_PROPERTY);
            // Only meaningful when the CK_JAR environment variable is also unset.
            if (System.getenv(CKToolRunner.CK_JAR_ENVIRONMENT_VARIABLE) == null) {
                assertEquals(Path.of(CKToolRunner.DEFAULT_CK_JAR_RELATIVE_PATH),
                        CKToolRunner.resolveCkJar());
            }
        } finally {
            if (previous != null) {
                System.setProperty(CKToolRunner.CK_JAR_PROPERTY, previous);
            }
        }
    }
}