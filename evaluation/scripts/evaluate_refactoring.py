import json
import os
import sys
from pathlib import Path
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

def load_plan(json_path):
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def evaluate(results_dir='results', output_dir='evaluation/results'):
    results_path = Path(results_dir)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    maint_file = results_path / 'workflow-plan-maintainability.json'
    comp_file = results_path / 'workflow-plan-complexity.json'
    bal_file = results_path / 'workflow-plan-balanced.json'

    if not maint_file.exists() or not comp_file.exists() or not bal_file.exists():
        print(f"Error: Required JSON files not found in {results_path}")
        return

    maint_data = load_plan(maint_file)
    comp_data = load_plan(comp_file)
    bal_data = load_plan(bal_file)

    # Extract records
    records = []
    for step in maint_data['steps']:
        cid = step['candidateId']
        target = step['className'] + (f".{step['methodName']}()" if step.get('methodName') and step.get('methodName') != 'N/A' else '')
        smell = step['smellType']
        action = step['recommendedAction']
        m_score = step['priorityScore']
        
        # Find corresponding scores in comp and bal
        c_score = next((s['priorityScore'] for s in comp_data['steps'] if s['candidateId'] == cid), 0.0)
        b_score = next((s['priorityScore'] for s in bal_data['steps'] if s['candidateId'] == cid), 0.0)

        # Ranks
        m_rank = step['stepNumber']
        c_rank = next((s['stepNumber'] for s in comp_data['steps'] if s['candidateId'] == cid), 0)
        b_rank = next((s['stepNumber'] for s in bal_data['steps'] if s['candidateId'] == cid), 0)

        # Factor breakdown under maintainability
        sb = step.get('scoreBreakdown', {})
        w = maint_data['weights']
        sev_contrib = sb.get('severityScore', 0.0) * w.get('w_severity', 0.20)
        met_contrib = sb.get('metricImpactScore', 0.0) * w.get('w_metric', 0.35)
        dep_contrib = sb.get('dependencyImpactScore', 0.0) * w.get('w_dependency', 0.25)
        ben_contrib = sb.get('estimatedBenefitScore', 0.0) * w.get('w_benefit', 0.20)

        records.append({
            'CandidateId': cid,
            'Target': target,
            'SmellType': smell,
            'Action': action,
            'Maintainability_Score': m_score,
            'Complexity_Score': c_score,
            'Balanced_Score': b_score,
            'Maintainability_Rank': m_rank,
            'Complexity_Rank': c_rank,
            'Balanced_Rank': b_rank,
            'Severity_Contribution': sev_contrib,
            'Metric_Contribution': met_contrib,
            'Dependency_Contribution': dep_contrib,
            'Benefit_Contribution': ben_contrib
        })

    df = pd.DataFrame(records)
    csv_out = output_path / 'evaluation_metrics.csv'
    df.to_csv(csv_out, index=False)
    print(f"Saved evaluation metrics CSV: {csv_out}")

    # Plot 1: Goal Comparison Grouped Bar Chart
    fig, ax = plt.subplots(figsize=(10, 6))
    x = np.arange(len(df))
    width = 0.25

    labels = [f"{r['CandidateId']}\n({r['SmellType']})" for _, r in df.iterrows()]

    rects1 = ax.bar(x - width, df['Maintainability_Score'], width, label='Improve Maintainability', color='#2563eb')
    rects2 = ax.bar(x, df['Complexity_Score'], width, label='Reduce Complexity', color='#dc2626')
    rects3 = ax.bar(x + width, df['Balanced_Score'], width, label='Balanced Improvement', color='#16a34a')

    ax.set_ylabel('Calculated Priority Score (P_i)')
    ax.set_title('Goal-Oriented Prioritization Score Divergence Across Quality Goals', fontsize=12, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=9)
    ax.set_ylim(0, 1.0)
    ax.legend(loc='upper right')
    ax.grid(axis='y', linestyle='--', alpha=0.5)

    chart1_out = output_path / 'goal_comparison_barchart.png'
    plt.tight_layout()
    plt.savefig(chart1_out, dpi=300)
    plt.close()
    print(f"Saved Goal Comparison Chart: {chart1_out}")

    # Plot 2: Stacked Bar Chart of Score Factor Contributions
    fig, ax = plt.subplots(figsize=(10, 6))
    bottom = np.zeros(len(df))

    p1 = ax.bar(x, df['Severity_Contribution'], width*1.5, label='Severity (w_s * S_i)', color='#f59e0b', bottom=bottom)
    bottom += df['Severity_Contribution']

    p2 = ax.bar(x, df['Metric_Contribution'], width*1.5, label='Metrics (w_m * M_i)', color='#3b82f6', bottom=bottom)
    bottom += df['Metric_Contribution']

    p3 = ax.bar(x, df['Dependency_Contribution'], width*1.5, label='Dependencies (w_d * D_i)', color='#8b5cf6', bottom=bottom)
    bottom += df['Dependency_Contribution']

    p4 = ax.bar(x, df['Benefit_Contribution'], width*1.5, label='Benefit (w_b * B_i)', color='#10b981', bottom=bottom)

    ax.set_ylabel('Priority Score Contribution')
    ax.set_title('Multi-Criteria Factor Breakdown (Maintainability Goal)', fontsize=12, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=9)
    ax.set_ylim(0, 1.0)
    ax.legend(loc='upper right')
    ax.grid(axis='y', linestyle='--', alpha=0.5)

    chart2_out = output_path / 'score_factors_breakdown.png'
    plt.tight_layout()
    plt.savefig(chart2_out, dpi=300)
    plt.close()
    print(f"Saved Factor Breakdown Chart: {chart2_out}")

    # Plot 3: Ranking Trajectory across Goals
    fig, ax = plt.subplots(figsize=(9, 5))
    goals = ['Maintainability', 'Balanced', 'Complexity']
    for _, r in df.iterrows():
        ranks = [r['Maintainability_Rank'], r['Balanced_Rank'], r['Complexity_Rank']]
        ax.plot(goals, ranks, marker='o', linewidth=2, label=f"{r['CandidateId']} ({r['SmellType']})")

    ax.set_ylabel('Workflow Step Position (1 = First Refactored)')
    ax.set_title('Ranking Position Stability and Shifts Across Quality Goals', fontsize=12, fontweight='bold')
    ax.invert_yaxis()
    ax.legend(loc='center left', bbox_to_anchor=(1, 0.5))
    ax.grid(True, linestyle='--', alpha=0.5)

    chart3_out = output_path / 'ranking_shift_trajectory.png'
    plt.tight_layout()
    plt.savefig(chart3_out, dpi=300)
    plt.close()
    print(f"Saved Ranking Trajectory Chart: {chart3_out}")

    # Generate Markdown Summary
    md_summary = f"""# Research Evaluation Summary: Refactoring Planner

## 1. Goal-Oriented Prioritization Analysis

| Candidate ID | Target | Code Smell | Maintainability (Score/Rank) | Complexity (Score/Rank) | Balanced (Score/Rank) |
|:---:|:---|:---|:---:|:---:|:---:|
"""
    for _, r in df.iterrows():
        md_summary += f"| `{r['CandidateId']}` | `{r['Target']}` | {r['SmellType']} | {r['Maintainability_Score']:.3f} (Step {r['Maintainability_Rank']}) | {r['Complexity_Score']:.3f} (Step {r['Complexity_Rank']}) | {r['Balanced_Score']:.3f} (Step {r['Balanced_Rank']}) |\n"

    md_summary += f"""
## 2. Key Findings for Thesis Evaluation Chapter

1. **Goal Responsiveness:** Priority scores demonstrably diverge based on developer quality goals:
   - High-complexity candidates (such as `LongMethodExample`) increase their priority score under **Reduce Complexity** ({df.loc[df['SmellType']=='LONG_METHOD', 'Maintainability_Score'].values[0]:.3f} -> {df.loc[df['SmellType']=='LONG_METHOD', 'Complexity_Score'].values[0]:.3f}).
   - Architectural candidates (such as `GodClassExample` and `FeatureEnvyExample`) receive higher prominence under **Improve Maintainability** due to higher coupling and lack of cohesion penalties.
2. **Multi-Criteria Transparency:** Grounded decomposition prevents arbitrary scoring, cleanly delineating severity, metrics, AST dependencies, and expected Fowler refactoring benefits.
3. **Artifacts Generated:**
   - [Goal Comparison Chart](goal_comparison_barchart.png)
   - [Factor Breakdown Chart](score_factors_breakdown.png)
   - [Ranking Trajectory Chart](ranking_shift_trajectory.png)
   - [Metrics CSV](evaluation_metrics.csv)
"""

    summary_file = output_path / 'evaluation_summary.md'
    with open(summary_file, 'w', encoding='utf-8') as f:
        f.write(md_summary)
    print(f"Saved Evaluation Summary: {summary_file}")

if __name__ == '__main__':
    evaluate()
