package com.amie.refactor.analysis;

import java.io.IOException;
import java.nio.file.Path;

public class PmdRunner {

    public void runPmd(Path projectDirectory)
            throws IOException, InterruptedException {

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String mavenExecutable = isWindows ? "mvn.cmd" : "mvn";

        ProcessBuilder processBuilder =
                new ProcessBuilder(
                        mavenExecutable,
                        "pmd:pmd"
                );

        processBuilder.directory(
                projectDirectory.toFile()
        );

        processBuilder.redirectErrorStream(true);

        processBuilder.inheritIO();

        Process process =
                processBuilder.start();

        int exitCode =
                process.waitFor();

        if (exitCode != 0) {

            throw new IllegalStateException(
                    "PMD execution failed. Exit code: "
                            + exitCode
            );
        }
    }
}