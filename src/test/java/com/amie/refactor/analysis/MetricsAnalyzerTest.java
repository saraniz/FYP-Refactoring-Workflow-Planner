package com.amie.refactor.analysis;

import com.amie.refactor.model.MetricsAnalysisResult;
import com.amie.refactor.model.MetricsAnalysisStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the orchestration performed by {@link MetricsAnalyzer} without a real CK Tool:
 * a fake {@link ProcessExecutor} plays the role of the tool and writes a controlled
 * {@code class.csv}.
 */
class MetricsAnalyzerTest {

    @TempDir
    Path tempDir;

    private Path sourceDirectoryWithJavaFile() throws IOException {
        Path source = tempDir.resolve("src");
        Files.createDirectories(source.resolve("demo"));
        Files.writeString(source.resolve("demo").resolve("Demo.java"), "package demo; class Demo {}");
        return source;
    }

    private MetricsAnalyzer analyzerWriting(String classCsvContent) throws IOException {
        Path jar = tempDir.resolve("ck.jar");
        Files.writeString(jar, "placeholder");
        Path output = tempDir.resolve("out");

        CKToolRunner runner = new CKToolRunner(
                jar, output, Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) -> {
                    Path outDir = Path.of(command.get(command.size() - 1));
                    Files.createDirectories(outDir);
                    Files.writeString(outDir.resolve("class.csv"), classCsvContent);
                    return new ProcessExecutor.Result(0, "Metrics extracted!!!", "", false);
                },
                "java");

        return new MetricsAnalyzer(runner, new MetricsCsvParser());
    }

    @Test
    void failsForNonexistentDirectory() {
        MetricsAnalysisResult result = new MetricsAnalyzer()
                .analyze(tempDir.resolve("does-not-exist"));

        assertEquals(MetricsAnalysisStatus.FAILED, result.getStatus());
        assertTrue(result.getErrors().get(0).contains("does not exist"));
    }

    @Test
    void failsWhenNoJavaSourcesArePresent() throws Exception {
        Path emptySource = tempDir.resolve("empty-src");
        Files.createDirectories(emptySource);

        MetricsAnalysisResult result = new MetricsAnalyzer().analyze(emptySource);

        assertEquals(MetricsAnalysisStatus.FAILED, result.getStatus());
        assertTrue(result.getErrors().get(0).contains("No Java source files"));
    }

    @Test
    void runsToolAndParsesMetricsOnSuccess() throws Exception {
        MetricsAnalyzer analyzer = analyzerWriting(
                "class,cbo,wmc,lcom\ndemo.Demo,3,4,1\n");

        MetricsAnalysisResult result = analyzer.analyze(sourceDirectoryWithJavaFile());

        assertEquals(MetricsAnalysisStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getClassMetrics().size());
        assertEquals(3, result.getClassMetrics().get(0).getCbo());
    }

    @Test
    void reportsPartialSuccessWhenAMetricValueIsMissing() throws Exception {
        MetricsAnalyzer analyzer = analyzerWriting(
                "class,cbo,wmc,lcom\ndemo.Demo,3,,1\n");

        MetricsAnalysisResult result = analyzer.analyze(sourceDirectoryWithJavaFile());

        assertEquals(MetricsAnalysisStatus.PARTIAL, result.getStatus());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("wmc")));
    }

    @Test
    void reportsFailureWhenTheToolFails() throws Exception {
        Path jar = tempDir.resolve("ck.jar");
        Files.writeString(jar, "placeholder");
        CKToolRunner runner = new CKToolRunner(
                jar, tempDir.resolve("out"), Duration.ofSeconds(10), false,
                (command, workingDirectory, timeout) ->
                        new ProcessExecutor.Result(1, "", "CK crashed", false),
                "java");

        MetricsAnalysisResult result = new MetricsAnalyzer(runner, new MetricsCsvParser())
                .analyze(sourceDirectoryWithJavaFile());

        assertEquals(MetricsAnalysisStatus.FAILED, result.getStatus());
        assertTrue(result.getErrors().get(0).contains("exit code 1"));
    }
}