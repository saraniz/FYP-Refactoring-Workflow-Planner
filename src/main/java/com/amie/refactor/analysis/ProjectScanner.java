package com.amie.refactor.analysis;

import java.io.IOException; //input output exception..if any file operation failed then java can throw IOException
import java.nio.file.Files;
import java.nio.file.Path; //path represents a file or directory location
import java.util.List;
import java.util.stream.Stream; // A Stream lets you process a collection of data step-by-step.

public class ProjectScanner {

    // This method will return a List containing Path objects.
    public List<Path> findJavaFiles(Path projectDirectory)
            throws IOException {

        // Check whether the given path is a directory
        if (!Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException(
                    "Invalid project directory: " + projectDirectory
            );
        }

        // Walk through the project directory recursively
        try (Stream<Path> paths = Files.walk(projectDirectory)) {

            return paths
                    // Keep only actual files
                    .filter(Files::isRegularFile)

                    // Keep only Java source files
                    .filter(path ->
                            path.toString().endsWith(".java"))

                    // Ignore generated files inside target
                    .filter(path ->
                            !path.toString().contains("\\target\\"))

                    // Convert the Stream into a List
                    .toList();
        }
    }
}