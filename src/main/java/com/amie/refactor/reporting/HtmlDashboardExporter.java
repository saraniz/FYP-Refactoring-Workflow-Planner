package com.amie.refactor.reporting;

import com.amie.refactor.diff.RefactoringDiff;
import com.amie.refactor.explanation.WorkflowStep;
import com.amie.refactor.forecast.BlastRadiusInfo;
import com.amie.refactor.forecast.MetricForecast;
import com.amie.refactor.planning.RefactoringCandidate;
import com.amie.refactor.planning.WorkflowPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Generates an interactive, standalone HTML dashboard presenting prioritized workflows,
 * "What-If" metric forecasts, effort/blast radius indicators, and side-by-side code diffs.
 */
public class HtmlDashboardExporter {

    public Path export(
            WorkflowPlan maintPlan, List<WorkflowStep> maintSteps,
            WorkflowPlan compPlan, List<WorkflowStep> compSteps,
            WorkflowPlan balPlan, List<WorkflowStep> balSteps,
            Path outputFile) throws IOException {

        Objects.requireNonNull(outputFile, "outputFile must not be null");
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }

        StringBuilder html = new StringBuilder();
        html.append("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>RefactorFlow Dashboard — Code-Aware Refactoring Planner</title>
    <style>
        :root {
            --primary: #1e3a8a;
            --primary-light: #3b82f6;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border: #e2e8f0;
            --green: #10b981;
            --amber: #f59e0b;
            --red: #ef4444;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
        body { background: var(--bg); color: var(--text-main); padding: 24px; }
        .header { background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%); color: white; padding: 24px 32px; border-radius: 12px; margin-bottom: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
        .header h1 { font-size: 24px; font-weight: 700; margin-bottom: 6px; }
        .header p { font-size: 14px; opacity: 0.9; }
        .goal-bar { display: flex; align-items: center; gap: 12px; background: white; padding: 12px 20px; border-radius: 10px; border: 1px solid var(--border); margin-bottom: 24px; }
        .goal-btn { padding: 8px 16px; border: 1px solid var(--border); background: white; border-radius: 6px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; color: var(--text-main); }
        .goal-btn.active { background: var(--primary); color: white; border-color: var(--primary); }
        .step-card { background: white; border-radius: 10px; border: 1px solid var(--border); margin-bottom: 18px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .step-header { padding: 16px 20px; background: #f1f5f9; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); }
        .step-title { font-size: 16px; font-weight: 700; color: var(--primary); }
        .badge { padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700; margin-left: 4px; }
        .badge-score { background: #dbeafe; color: #1e40af; }
        .badge-smell { background: #ccfbf1; color: #0f766e; }
        .step-body { padding: 20px; }
        .forecast-box { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; }
        .forecast-header { font-size: 12px; font-weight: 700; text-transform: uppercase; color: #166534; margin-bottom: 8px; }
        .forecast-pills { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 6px; }
        .forecast-pill { background: white; border: 1px solid #86efac; border-radius: 4px; padding: 4px 10px; font-size: 12px; font-weight: 600; color: #14532d; }
        .forecast-desc { font-size: 12.5px; color: #166534; font-style: italic; }
        .blast-box { background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px; padding: 12px 16px; margin-bottom: 14px; font-size: 12.5px; color: #9a3412; }
        .explanation-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 16px; font-size: 13.5px; line-height: 1.5; }
        .diff-container { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 14px; }
        .diff-pane { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; }
        .diff-pane-header { padding: 8px 12px; font-size: 12px; font-weight: 700; text-transform: uppercase; }
        .diff-before-header { background: #fee2e2; color: #991b1b; }
        .diff-after-header { background: #dcfce7; color: #166534; }
        .diff-code { padding: 12px; font-family: 'Consolas', 'Courier New', monospace; font-size: 12px; line-height: 1.45; white-space: pre-wrap; background: #fafafa; max-height: 280px; overflow-y: auto; }
        .toggle-btn { background: #e0e7ff; color: #3730a3; border: none; padding: 6px 12px; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; margin-top: 10px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>RefactorFlow: Code-Aware Refactoring Planner</h1>
        <p>Goal-Oriented Prioritization, What-If Metric Forecasting, Blast Radius Analysis & Native Diff Previews</p>
    </div>

    <div class="goal-bar">
        <span style="font-weight:700; font-size:13px; color:var(--text-muted);">SELECT QUALITY GOAL:</span>
        <button class="goal-btn active" onclick="switchGoal('maint', this)">Improve Maintainability</button>
        <button class="goal-btn" onclick="switchGoal('comp', this)">Reduce Complexity</button>
        <button class="goal-btn" onclick="switchGoal('bal', this)">Balanced Improvement</button>
    </div>

    <div id="maint-view">
""");

        renderStepCards(html, maintSteps);
        html.append("    </div>\n    <div id=\"comp-view\" style=\"display:none;\">\n");
        renderStepCards(html, compSteps);
        html.append("    </div>\n    <div id=\"bal-view\" style=\"display:none;\">\n");
        renderStepCards(html, balSteps);
        html.append("""
    </div>

    <script>
        function switchGoal(goal, btn) {
            document.querySelectorAll('.goal-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            document.getElementById('maint-view').style.display = goal === 'maint' ? 'block' : 'none';
            document.getElementById('comp-view').style.display = goal === 'comp' ? 'block' : 'none';
            document.getElementById('bal-view').style.display = goal === 'bal' ? 'block' : 'none';
        }
        function toggleDiff(id) {
            const el = document.getElementById(id);
            el.style.display = el.style.display === 'none' ? 'grid' : 'none';
        }
    </script>
</body>
</html>
""");

        Files.writeString(outputFile, html.toString());
        return outputFile;
    }

    private void renderStepCards(StringBuilder html, List<WorkflowStep> steps) {
        for (WorkflowStep step : steps) {
            RefactoringCandidate c = step.getCandidate();
            RefactoringDiff diff = c.getDiffPreview();
            MetricForecast fc = c.getMetricForecast();
            BlastRadiusInfo br = c.getBlastRadius();
            String diffId = "diff-" + step.getStepNumber() + "-" + c.getCandidateId();

            html.append("        <div class=\"step-card\">\n");
            html.append("            <div class=\"step-header\">\n");
            html.append("                <div class=\"step-title\">Step ").append(step.getStepNumber()).append(": ").append(escapeHtml(step.getTitle())).append("</div>\n");
            html.append("                <div>\n");
            html.append("                    <span class=\"badge badge-score\">Score: ").append(String.format("%.3f", c.getPriorityScore())).append("</span>\n");
            html.append("                    <span class=\"badge badge-smell\">").append(c.getSmellType()).append("</span>\n");

            if (br != null) {
                html.append("                    <span class=\"badge\" style=\"background:").append(br.getColorHex()).append("20; color:").append(br.getColorHex()).append(";\">")
                        .append(br.getRiskBadge()).append("</span>\n");
            }

            html.append("                </div>\n");
            html.append("            </div>\n");

            html.append("            <div class=\"step-body\">\n");

            // What-If Metric Forecast Banner
            if (fc != null) {
                html.append("                <div class=\"forecast-box\">\n");
                html.append("                    <div class=\"forecast-header\">📊 Simulated \"What-If\" Metric Forecast</div>\n");
                html.append("                    <div class=\"forecast-pills\">\n");
                html.append("                        <span class=\"forecast-pill\">WMC (Complexity): ").append(fc.getWmcBefore()).append(" &rarr; ").append(fc.getWmcAfter())
                        .append(fc.getWmcReductionPct() > 0 ? " (-" + fc.getWmcReductionPct() + "%)" : "").append("</span>\n");
                html.append("                        <span class=\"forecast-pill\">LCOM (Cohesion): ").append(fc.getLcomBefore()).append(" &rarr; ").append(fc.getLcomAfter())
                        .append(fc.getLcomReductionPct() > 0 ? " (-" + fc.getLcomReductionPct() + "%)" : "").append("</span>\n");
                html.append("                        <span class=\"forecast-pill\">CBO (Coupling): ").append(fc.getCboBefore()).append(" &rarr; ").append(fc.getCboAfter()).append("</span>\n");
                if (fc.getParamCountBefore() > 0) {
                    html.append("                        <span class=\"forecast-pill\">Parameters: ").append(fc.getParamCountBefore()).append(" &rarr; ").append(fc.getParamCountAfter()).append("</span>\n");
                }
                html.append("                    </div>\n");
                html.append("                    <div class=\"forecast-desc\">").append(escapeHtml(fc.getForecastSummary())).append("</div>\n");
                html.append("                </div>\n");
            }

            // Blast Radius Banner
            if (br != null && !br.getAffectedCallers().isEmpty()) {
                html.append("                <div class=\"blast-box\">\n");
                html.append("                    <b>Effort / Blast Radius:</b> ").append(escapeHtml(br.getRiskRationale())).append("<br/>\n");
                html.append("                    <b>Affected Callers:</b> <code>").append(String.join("</code>, <code>", br.getAffectedCallers())).append("</code>\n");
                html.append("                </div>\n");
            }

            html.append("                <div class=\"explanation-box\">\n");
            html.append("                    <p><b>Target Location:</b> <code>").append(c.getFilePath()).append("</code> (lines ").append(c.getStartLine()).append("-").append(c.getEndLine()).append(")</p>\n");
            html.append("                    <p style=\"margin-top:6px;\"><b>Why Recommended:</b> ").append(escapeHtml(step.getExplanation().getWhyRecommended())).append("</p>\n");
            html.append("                    <p style=\"margin-top:6px;\"><b>Why in this Sequence:</b> ").append(escapeHtml(step.getExplanation().getWhyInThisSequence())).append("</p>\n");
            html.append("                </div>\n");

            if (diff != null) {
                html.append("                <button class=\"toggle-btn\" onclick=\"toggleDiff('").append(diffId).append("')\">Toggle Side-by-Side Diff Preview</button>\n");
                html.append("                <div id=\"").append(diffId).append("\" class=\"diff-container\" style=\"display:grid;\">\n");
                html.append("                    <div class=\"diff-pane\">\n");
                html.append("                        <div class=\"diff-pane-header diff-before-header\">Original (Before Refactoring)</div>\n");
                html.append("                        <div class=\"diff-code\">").append(escapeHtml(diff.getBeforeCode())).append("</div>\n");
                html.append("                    </div>\n");
                html.append("                    <div class=\"diff-pane\">\n");
                html.append("                        <div class=\"diff-pane-header diff-after-header\">Proposed Refactoring (After)</div>\n");
                html.append("                        <div class=\"diff-code\">").append(escapeHtml(diff.getAfterCode())).append("</div>\n");
                html.append("                    </div>\n");
                html.append("                </div>\n");
            }

            html.append("            </div>\n");
            html.append("        </div>\n");
        }
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
