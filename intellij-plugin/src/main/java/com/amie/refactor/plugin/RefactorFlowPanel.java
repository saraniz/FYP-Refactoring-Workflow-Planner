package com.amie.refactor.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main UI panel for the RefactorFlow IntelliJ tool window.
 * Displays goal switcher, prioritized workflow steps, "What-If" metric forecasts,
 * effort / blast radius indicators, diff comparisons, and source navigation.
 */
public class RefactorFlowPanel {

    private final Project project;
    private final JBPanel<?> mainPanel;
    private final JBPanel<?> stepsContainer;
    private final JComboBox<String> goalComboBox;
    private final JBLabel statusLabel;

    public RefactorFlowPanel(Project project) {
        this.project = project;
        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.mainPanel.setBorder(JBUI.Borders.empty(10));

        // Header Section
        JBPanel<?> headerPanel = new JBPanel<>(new BorderLayout());
        JBLabel titleLabel = new JBLabel("<html><b>RefactorFlow Planner</b><br/><small style='color:#64748b;'>Goal-Oriented & Explainable Java Refactoring</small></html>");
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        // Goal Selection & Action Bar
        JBPanel<?> controlsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 5, 5));
        controlsPanel.add(new JBLabel("Quality Goal:"));
        goalComboBox = new JComboBox<>(new String[]{
                "Improve Maintainability",
                "Reduce Complexity",
                "Balanced Improvement"
        });
        goalComboBox.addActionListener(e -> refreshWorkflow());
        controlsPanel.add(goalComboBox);

        JButton refreshButton = new JButton("Reload Plan");
        refreshButton.setToolTipText("Reload prioritized plan from results directory");
        refreshButton.addActionListener(e -> refreshWorkflow());
        controlsPanel.add(refreshButton);

        headerPanel.add(controlsPanel, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JBLabel("Ready");
        statusLabel.setForeground(new Color(100, 116, 139));
        statusLabel.setBorder(JBUI.Borders.empty(2, 5, 5, 5));
        headerPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Steps Container (Scrollable)
        stepsContainer = new JBPanel<>();
        stepsContainer.setLayout(new BoxLayout(stepsContainer, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane(stepsContainer);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Load initial plan
        refreshWorkflow();
    }

    public JComponent getContent() {
        return mainPanel;
    }

    private void refreshWorkflow() {
        stepsContainer.removeAll();

        String selectedGoal = (String) goalComboBox.getSelectedItem();
        String jsonFileName = "workflow-plan-maintainability.json";
        if ("Reduce Complexity".equals(selectedGoal)) {
            jsonFileName = "workflow-plan-complexity.json";
        } else if ("Balanced Improvement".equals(selectedGoal)) {
            jsonFileName = "workflow-plan-balanced.json";
        }

        Path basePath = Path.of(project.getBasePath() != null ? project.getBasePath() : ".");
        
        // Search in common results locations
        Path jsonPath = basePath.resolve("results").resolve(jsonFileName);
        if (!Files.exists(jsonPath)) {
            jsonPath = basePath.resolve("refactoring-workflow-planner/results").resolve(jsonFileName);
        }

        if (Files.exists(jsonPath)) {
            try {
                String jsonContent = Files.readString(jsonPath);
                int count = renderStepsFromJson(jsonContent);
                statusLabel.setText(String.format("Loaded %d steps for %s from %s", count, selectedGoal, jsonPath.getFileName()));
            } catch (Exception ex) {
                renderErrorCard("Failed to read plan: " + ex.getMessage());
                statusLabel.setText("Error loading plan file.");
            }
        } else {
            renderSampleSteps();
            statusLabel.setText("Showing illustrative preview (run analysis to generate live project plan).");
        }

        stepsContainer.revalidate();
        stepsContainer.repaint();
    }

    private int renderStepsFromJson(String jsonContent) {
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
        if (!root.has("steps") || !root.get("steps").isJsonArray()) {
            renderErrorCard("Invalid workflow JSON format: missing 'steps' array.");
            return 0;
        }

        JsonArray stepsArray = root.getAsJsonArray("steps");
        if (stepsArray.isEmpty()) {
            JBLabel emptyLabel = new JBLabel("<html><i>No refactoring candidates detected in project.</i></html>");
            emptyLabel.setBorder(JBUI.Borders.empty(15));
            stepsContainer.add(emptyLabel);
            return 0;
        }

        for (int i = 0; i < stepsArray.size(); i++) {
            JsonObject stepObj = stepsArray.get(i).getAsJsonObject();
            int stepNum = stepObj.has("stepNumber") ? stepObj.get("stepNumber").getAsInt() : (i + 1);
            String title = stepObj.has("title") ? stepObj.get("title").getAsString() : "Step " + stepNum;
            String smellType = stepObj.has("smellType") ? stepObj.get("smellType").getAsString() : "CODE_SMELL";
            String action = stepObj.has("recommendedAction") ? stepObj.get("recommendedAction").getAsString() : "";
            double priorityScore = stepObj.has("priorityScore") ? stepObj.get("priorityScore").getAsDouble() : 0.0;
            String filePath = stepObj.has("filePath") ? stepObj.get("filePath").getAsString() : "";
            int startLine = stepObj.has("startLine") ? stepObj.get("startLine").getAsInt() : 1;

            // Diff preview data
            String summary = "";
            String beforeCode = "";
            String afterCode = "";
            if (stepObj.has("diffPreview") && !stepObj.get("diffPreview").isJsonNull()) {
                JsonObject dp = stepObj.getAsJsonObject("diffPreview");
                summary = dp.has("summary") ? dp.get("summary").getAsString() : "";
                beforeCode = dp.has("beforeCode") ? dp.get("beforeCode").getAsString() : "";
                afterCode = dp.has("afterCode") ? dp.get("afterCode").getAsString() : "";
            }

            // What-If Metric Forecast
            String forecastHtml = "";
            if (stepObj.has("metricForecast") && !stepObj.get("metricForecast").isJsonNull()) {
                JsonObject fc = stepObj.getAsJsonObject("metricForecast");
                int wmcBefore = fc.has("wmcBefore") ? fc.get("wmcBefore").getAsInt() : 0;
                int wmcAfter = fc.has("wmcAfter") ? fc.get("wmcAfter").getAsInt() : 0;
                double wmcRed = fc.has("wmcReductionPct") ? fc.get("wmcReductionPct").getAsDouble() : 0.0;
                int lcomBefore = fc.has("lcomBefore") ? fc.get("lcomBefore").getAsInt() : 0;
                int lcomAfter = fc.has("lcomAfter") ? fc.get("lcomAfter").getAsInt() : 0;
                double lcomRed = fc.has("lcomReductionPct") ? fc.get("lcomReductionPct").getAsDouble() : 0.0;
                int cboBefore = fc.has("cboBefore") ? fc.get("cboBefore").getAsInt() : 0;
                int cboAfter = fc.has("cboAfter") ? fc.get("cboAfter").getAsInt() : 0;
                String fcSummary = fc.has("forecastSummary") ? fc.get("forecastSummary").getAsString() : "";

                forecastHtml = String.format(
                        "<html><div style='background-color:#f0fdf4; border:1px solid #86efac; border-radius:4px; padding:5px 8px; margin:4px 0;'>" +
                        "<span style='color:#166534; font-weight:bold; font-size:10px;'>📊 WHAT-IF METRIC FORECAST:</span><br/>" +
                        "<span style='color:#14532d; font-size:11px;'><b>WMC:</b> %d &rarr; %d %s &nbsp;|&nbsp; <b>LCOM:</b> %d &rarr; %d %s &nbsp;|&nbsp; <b>CBO:</b> %d &rarr; %d</span><br/>" +
                        "<small style='color:#166534; font-style:italic;'>%s</small>" +
                        "</div></html>",
                        wmcBefore, wmcAfter, (wmcRed > 0 ? "(-" + (int) wmcRed + "%)" : ""),
                        lcomBefore, lcomAfter, (lcomRed > 0 ? "(-" + (int) lcomRed + "%)" : ""),
                        cboBefore, cboAfter,
                        fcSummary
                );
            }

            // Effort / Blast Radius Indicator
            String riskBadge = "🟢 Quick Win";
            String blastHtml = "";
            if (stepObj.has("blastRadius") && !stepObj.get("blastRadius").isJsonNull()) {
                JsonObject br = stepObj.getAsJsonObject("blastRadius");
                riskBadge = br.has("riskBadge") ? br.get("riskBadge").getAsString() : "🟢 Quick Win";
                int fanIn = br.has("fanIn") ? br.get("fanIn").getAsInt() : 0;
                int fanOut = br.has("fanOut") ? br.get("fanOut").getAsInt() : 0;
                String riskRationale = br.has("riskRationale") ? br.get("riskRationale").getAsString() : "";
                
                List<String> callers = new ArrayList<>();
                if (br.has("affectedCallers") && br.get("affectedCallers").isJsonArray()) {
                    for (JsonElement c : br.getAsJsonArray("affectedCallers")) {
                        callers.add(c.getAsString());
                    }
                }

                String callersStr = callers.isEmpty() ? "None (Safe Leaf Service)" : String.join(", ", callers);
                blastHtml = String.format(
                        "<html><div style='background-color:#fff7ed; border:1px solid #fed7aa; border-radius:4px; padding:4px 6px; margin:4px 0; font-size:10.5px; color:#9a3412;'>" +
                        "<b>Blast Radius:</b> %s (Fan-In: %d, Fan-Out: %d)<br/>" +
                        "<b>Callers to Verify:</b> <code>%s</code>" +
                        "</div></html>",
                        riskRationale, fanIn, fanOut, callersStr
                );
            }

            // Explanation details
            String whyPrioritized = "";
            String whyInSequence = "";
            String whyRecommended = "";
            StringBuilder guidanceHtml = new StringBuilder();
            if (stepObj.has("explanation") && !stepObj.get("explanation").isJsonNull()) {
                JsonObject exp = stepObj.getAsJsonObject("explanation");
                if (exp.has("whyPrioritized")) whyPrioritized = exp.get("whyPrioritized").getAsString();
                if (exp.has("whyInThisSequence")) whyInSequence = exp.get("whyInThisSequence").getAsString();
                if (exp.has("whyRecommended")) whyRecommended = exp.get("whyRecommended").getAsString();
                if (exp.has("actionableGuidance") && exp.get("actionableGuidance").isJsonArray()) {
                    JsonArray guidanceArray = exp.getAsJsonArray("actionableGuidance");
                    for (JsonElement g : guidanceArray) {
                        guidanceHtml.append("• ").append(g.getAsString()).append("<br/>");
                    }
                }
            }

            addStepCard(
                    stepNum,
                    title,
                    smellType,
                    priorityScore,
                    riskBadge,
                    summary,
                    forecastHtml,
                    blastHtml,
                    beforeCode,
                    afterCode,
                    filePath,
                    startLine,
                    whyPrioritized,
                    whyInSequence,
                    guidanceHtml.toString()
            );
        }

        return stepsArray.size();
    }

    private void renderSampleSteps() {
        addStepCard(
                1,
                "Extract Class in GodClassExample",
                "GOD_CLASS",
                0.483,
                "🟢 Quick Win",
                "Separate 13 methods and course management responsibilities into new class.",
                "<html><div style='background-color:#f0fdf4; border:1px solid #86efac; border-radius:4px; padding:4px 6px;'>" +
                "<b>📊 Forecast:</b> WMC: 13 &rarr; 6 (-54%) | LCOM: 78 &rarr; 19 (-75%) | CBO: 0 &rarr; 1" +
                "</div></html>",
                "<html><div style='background-color:#fff7ed; border:1px solid #fed7aa; border-radius:4px; padding:4px 6px; font-size:10.5px;'>" +
                "<b>Blast Radius:</b> Quick Win: Zero external callers; safe isolated refactoring." +
                "</div></html>",
                """
public class GodClassExample {
    private String studentName;
    private String courseName;
    private double courseFee;
    public void registerStudent() { ... }
    public void createCourse() { ... }
}""",
                """
// Extracted cohesive class:
public class CourseManager {
    private String courseName;
    private double courseFee;
    public void createCourse() { ... }
}
// Delegated original class:
public class GodClassExample {
    private CourseManager courseManager = new CourseManager();
    public void createCourse() { courseManager.createCourse(); }
}""",
                "",
                1,
                "Severity: 0.65 (God Class), Metric Impact: 0.45, Benefit: 0.98",
                "First step due to highest unblocked priority in dependency DAG.",
                "• Identify cohesive field access groupings.<br/>• Extract new class CourseManager.<br/>• Delegate calls from GodClassExample."
        );
    }

    private void addStepCard(
            int stepNum,
            String title,
            String smellType,
            double priorityScore,
            String riskBadge,
            String summary,
            String forecastHtml,
            String blastHtml,
            String beforeCode,
            String afterCode,
            String filePath,
            int startLine,
            String whyPrioritized,
            String whyInSequence,
            String actionableStepsHtml) {

        JBPanel<?> card = new JBPanel<>(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 4, 6, 4),
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1)
        ));
        card.setBackground(new Color(248, 250, 252));

        // Card Header
        JBPanel<?> cardHeader = new JBPanel<>(new BorderLayout());
        cardHeader.setBackground(new Color(241, 245, 249));
        cardHeader.setBorder(JBUI.Borders.empty(6));

        JBLabel stepLabel = new JBLabel(String.format("<html><b>Step %d: %s</b></html>", stepNum, title));
        
        String badgeColor = switch (smellType) {
            case "GOD_CLASS" -> "#c2410c";
            case "LONG_METHOD" -> "#1d4ed8";
            case "FEATURE_ENVY" -> "#7e22ce";
            case "DATA_CLUMPS" -> "#047857";
            default -> "#475569";
        };

        JBLabel scoreLabel = new JBLabel(String.format(
                "<html><span style='color:#2563eb;'><b>Score: %.3f</b></span> &nbsp; " +
                "<span style='background-color:%s; color:white; padding:2px 5px; border-radius:3px; font-size:10px;'><b>%s</b></span> &nbsp; " +
                "<span style='font-size:10px;'><b>%s</b></span></html>",
                priorityScore, badgeColor, smellType, riskBadge));
        cardHeader.add(stepLabel, BorderLayout.WEST);
        cardHeader.add(scoreLabel, BorderLayout.EAST);
        card.add(cardHeader, BorderLayout.NORTH);

        // Card Body
        JBPanel<?> cardBody = new JBPanel<>();
        cardBody.setLayout(new BoxLayout(cardBody, BoxLayout.Y_AXIS));
        cardBody.setBorder(JBUI.Borders.empty(8));
        cardBody.setBackground(Color.WHITE);

        if (!summary.isEmpty()) {
            JBLabel summaryLabel = new JBLabel("<html><p style='width:330px; color:#1e293b; margin-bottom:4px;'><b>Objective:</b> " + summary + "</p></html>");
            cardBody.add(summaryLabel);
        }

        if (!forecastHtml.isEmpty()) {
            JBLabel fcLabel = new JBLabel(forecastHtml);
            cardBody.add(fcLabel);
        }

        if (!blastHtml.isEmpty()) {
            JBLabel blastLabel = new JBLabel(blastHtml);
            cardBody.add(blastLabel);
        }

        if (!whyInSequence.isEmpty()) {
            JBLabel seqLabel = new JBLabel("<html><p style='width:330px; color:#475569; font-size:10px; margin-top:4px; margin-bottom:4px;'><b>Sequence Rationale:</b> " + whyInSequence + "</p></html>");
            cardBody.add(seqLabel);
        }

        if (!actionableStepsHtml.isEmpty()) {
            JBLabel stepsLabel = new JBLabel("<html><p style='width:330px; color:#334155; font-size:10px; margin-bottom:6px;'><b>Actionable Guidance:</b><br/>" + actionableStepsHtml + "</p></html>");
            cardBody.add(stepsLabel);
        }

        // Action Buttons Row
        JBPanel<?> actionsPanel = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 4, 2));
        actionsPanel.setBackground(Color.WHITE);

        if (!beforeCode.isEmpty() && !afterCode.isEmpty()) {
            JButton diffButton = new JButton("Compare Diff");
            diffButton.setToolTipText("Open native side-by-side IntelliJ diff viewer for this refactoring");
            diffButton.addActionListener(e ->
                    DiffViewerLauncher.showDiff(project, "Step " + stepNum + ": " + title, beforeCode, afterCode)
            );
            actionsPanel.add(diffButton);
        }

        if (!filePath.isEmpty()) {
            JButton jumpButton = new JButton("Jump to Code");
            jumpButton.setToolTipText("Open target source file at violation location");
            jumpButton.addActionListener(e -> {
                VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(filePath.replace('\\', '/'));
                if (vf != null) {
                    new OpenFileDescriptor(project, vf, Math.max(0, startLine - 1), 0).navigate(true);
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "Source file not found at: " + filePath, "File Not Found", JOptionPane.WARNING_MESSAGE);
                }
            });
            actionsPanel.add(jumpButton);
        }

        cardBody.add(actionsPanel);
        card.add(cardBody, BorderLayout.CENTER);

        stepsContainer.add(card);
    }

    private void renderErrorCard(String errorMsg) {
        JBLabel errLabel = new JBLabel("<html><span style='color:red;'>" + errorMsg + "</span></html>");
        errLabel.setBorder(JBUI.Borders.empty(10));
        stepsContainer.add(errLabel);
    }
}
