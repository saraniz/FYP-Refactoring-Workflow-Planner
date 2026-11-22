package com.amie.refactor.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Thin abstraction over starting and waiting for an external process.
 *
 * <p>It exists so that {@link CKToolRunner} can be unit tested (exit codes, timeouts,
 * missing output) without a real CK Tool installation: tests supply a controlled fake
 * implementation. Production code uses {@link #system()}.</p>
 */
@FunctionalInterface
public interface ProcessExecutor {

    Result execute(List<String> command, Path workingDirectory, Duration timeout)
            throws IOException, InterruptedException;

    /**
     * Outcome of running an external process.
     *
     * @param exitCode         the process exit code, or {@code -1} when it timed out
     * @param standardOutput   captured stdout (UTF-8)
     * @param standardError    captured stderr (UTF-8)
     * @param timedOut         true when the process was forcibly destroyed after the timeout
     */
    record Result(int exitCode, String standardOutput, String standardError, boolean timedOut) {

        public boolean succeeded() {
            return !timedOut && exitCode == 0;
        }
    }

    /** Default implementation backed by {@link ProcessBuilder}. */
    static ProcessExecutor system() {
        return new SystemProcessExecutor();
    }

    final class SystemProcessExecutor implements ProcessExecutor {

        @Override
        public Result execute(List<String> command, Path workingDirectory, Duration timeout)
                throws IOException, InterruptedException {

            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDirectory != null) {
                builder.directory(workingDirectory.toFile());
            }

            Process process = builder.start();

            // Drain stdout and stderr on separate threads so that a full pipe buffer
            // can never block the child process (a classic ProcessBuilder deadlock).
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();
            Thread stdoutThread = drain(process.getInputStream(), stdout);
            Thread stderrThread = drain(process.getErrorStream(), stderr);

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutThread.join(1000);
                stderrThread.join(1000);
                return new Result(-1, stdout.toString(), stderr.toString(), true);
            }

            stdoutThread.join();
            stderrThread.join();
            return new Result(process.exitValue(), stdout.toString(), stderr.toString(), false);
        }

        private static Thread drain(InputStream stream, StringBuilder sink) {
            Thread thread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sink.append(line).append(System.lineSeparator());
                    }
                } catch (IOException ignored) {
                    // The stream is closed when the process is forcibly destroyed.
                }
            }, "ck-stream-drain");
            thread.setDaemon(true);
            thread.start();
            return thread;
        }
    }
}