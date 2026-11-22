package com.amie.refactor.plugin;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.project.Project;

/**
 * Invokes IntelliJ IDEA's native Side-by-Side Diff Viewer.
 * Compares the original smelly code with the proposed refactored code.
 */
public class DiffViewerLauncher {

    public static void showDiff(
            Project project,
            String stepTitle,
            String beforeCode,
            String afterCode) {

        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        DiffContent contentBefore = contentFactory.create(beforeCode);
        DiffContent contentAfter = contentFactory.create(afterCode);

        SimpleDiffRequest request = new SimpleDiffRequest(
                "RefactorFlow Preview: " + stepTitle,
                contentBefore,
                contentAfter,
                "Original (Smelly Code)",
                "Proposed Refactoring"
        );

        DiffManager.getInstance().showDiff(project, request);
    }
}
