package com.amie.refactor.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the RefactorFlow tool window on the right sidebar of IntelliJ IDEA.
 */
public class RefactorFlowToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        RefactorFlowPanel panel = new RefactorFlowPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
