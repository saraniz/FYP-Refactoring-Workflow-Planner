package com.amie.refactor;

import com.amie.refactor.analysis.ProjectScanner;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectScannerTest {

    @Test
    void shouldFindJavaFiles() throws Exception {

        ProjectScanner scanner = new ProjectScanner();

        Path project =
                Path.of("sample-projects/smell-test-project1");

        List<Path> files =
                scanner.findJavaFiles(project);

        assertFalse(files.isEmpty());

        assertTrue(
                files.stream()
                        .anyMatch(path ->
                                path.toString()
                                        .endsWith("LongMethodExample.java"))
        );
    }
}
