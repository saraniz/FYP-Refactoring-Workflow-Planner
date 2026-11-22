# Code-Aware Refactoring Workflow Planner

A Goal-Oriented, Dependency-Aware, and Explainable Refactoring Workflow Planner for Java Systems.

## Project Overview

This system is an intelligent developer-support prototype designed for undergraduate thesis research at the University of Sri Jayewardenepura, Faculty of Computing. It analyzes Java projects to detect code smells, computes software metrics, extracts AST structures and dependency relationships, and produces an ordered, goal-oriented refactoring workflow accompanied by code-aware explanations.

## Five Research Objectives & Implementation Status

- [x] **Objective 1: Code Smell Detection Module (PMD)**
  - Detects the four target code smells: **God Class**, **Long Method**, **Feature Envy**, and **Data Clumps**.
  - Normalizes tool findings into a unified model.
- [x] **Objective 2: Metrics and Dependency Analysis Module**
  - **Part A (CK Tool)**: Calculates Coupling Between Objects (**CBO**), Weighted Methods per Class (**WMC**), and Lack of Cohesion of Methods (**LCOM**).
  - **Part B (JavaParser AST)**: Extracts class hierarchies, method definitions, field references, call graphs, and computes fine-grained **Fan-In** and **Fan-Out** coupling metrics.
- [x] **Objective 3: Goal-Oriented Workflow Planning Engine**
  - Incorporates developer-selected quality goals: **Improve Maintainability**, **Reduce Complexity**, and **Balanced Improvement**.
  - Deterministic multi-criteria scoring function: \(P_i = w_s S_i + w_m M_i + w_d D_i + w_b B_i\).
  - Dependency-aware topological sort over a Directed Acyclic Graph (DAG) with a Max-Priority Queue (Kahn's algorithm).
- [x] **Objective 4: Code-Aware Explanation Generator**
  - Grounded in verified AST facts, exact source lines, CK metric values, and caller/callee context (What, Where, Why, Why Prioritized, Sequence Justification, and Actionable Steps).
- [x] **Objective 5: Testing, Evaluation & Research Analytics**
  - Unit and integration tests (JUnit 5).
  - JSON and Markdown reporting pipelines.
  - Python evaluation scripts using Pandas and Matplotlib for comparative benchmarking, score distributions, and rank trajectory visualization.

## Technology Stack

- **Core Application**: Java 26, Maven 3.9+
- **Static Analysis & Smells**: PMD 7
- **Software Metrics**: CK Tool 0.7.0
- **AST & Dependency Analysis**: JavaParser 3.26.3
- **Serialization & Reporting**: Google Gson 2.11.0, Apache Commons CSV
- **Research Evaluation & Visualizations**: Python 3.10+, Pandas, Matplotlib

## Project Layout

```
refactoring-workflow-planner/
├── pom.xml
├── README.md
├── src/
│   ├── main/java/com/amie/refactor/
│   │   ├── Main.java                               # Complete CLI orchestration pipeline
│   │   ├── analysis/
│   │   │   ├── PmdRunner.java                      # PMD executor
│   │   │   ├── PmdResultParser.java                # PMD XML parser
│   │   │   ├── CKToolRunner.java                   # CK Tool process runner
│   │   │   ├── MetricsCsvParser.java               # CK CSV parser
│   │   │   ├── MetricsAnalyzer.java                # Metrics coordinator
│   │   │   ├── SmellMetricsIntegrator.java         # Integrates PMD & CK metrics
│   │   │   └── dependency/
│   │   │       └── JavaParserDependencyAnalyzer.java # AST & Dependency Graph analyzer
│   │   ├── model/
│   │   │   ├── CodeSmell.java                      # Normalized smell model
│   │   │   ├── SmellType.java                      # Target smell types enum
│   │   │   ├── ClassMetrics.java                   # CK metrics model (CBO, WMC, LCOM)
│   │   │   ├── ClassAnalysis.java                  # Combined per-class findings
│   │   │   └── dependency/
│   │   │       ├── Dependency.java                 # Directed dependency relation
│   │   │       ├── DependencyType.java             # CALLS, EXTENDS, CONTAINS, etc.
│   │   │       ├── MethodStructure.java            # Method lines, complexity, calls
│   │   │       ├── ClassStructure.java             # Class hierarchy, fan-in/fan-out
│   │   │       └── DependencyAnalysisResult.java   # AST analysis output
│   │   ├── planning/
│   │   │   ├── QualityGoal.java                    # Maintainability, Complexity, Balanced
│   │   │   ├── RefactoringAction.java              # Extract Method, Extract Class, etc.
│   │   │   ├── GoalWeights.java                    # Scoring weights preset & custom
│   │   │   ├── ScoringConfiguration.java           # Thresholds and saturation bounds
│   │   │   ├── ScoreBreakdown.java                 # Traceable score component breakdown
│   │   │   ├── RefactoringCandidate.java           # Prioritized candidate model
│   │   │   ├── PriorityScorer.java                 # Multi-criteria scoring engine
│   │   │   ├── RefactoringDependencyGraph.java     # DAG with topological priority sort
│   │   │   ├── WorkflowPlan.java                   # Resulting planned workflow
│   │   │   └── WorkflowPlanner.java                # Planning coordinator
│   │   ├── explanation/
│   │   │   ├── StepExplanation.java                # Structured explanation sections
│   │   │   ├── WorkflowStep.java                   # Step container
│   │   │   └── ExplanationGenerator.java           # Code-grounded explanation synthesizer
│   │   └── reporting/
│   │       ├── JsonExporter.java                   # JSON export for Python & tools
│   │       └── MarkdownReportExporter.java         # GitHub markdown workflow report
│   └── test/java/com/amie/refactor/               # Comprehensive automated test suite
├── sample-projects/                                # Test projects with deliberate smells
├── results/                                        # Exported JSON and Markdown reports
├── evaluation/
│   ├── scripts/
│   │   └── evaluate_refactoring.py                 # Pandas/Matplotlib evaluation script
│   └── results/                                    # Generated charts and summary CSV
└── docs/
    ├── metrics-analysis.md                         # Detailed documentation of CK module
    └── research-decision-phases.md                 # Configurable research parameters guide
```

## How to Run

### 1. Compile and Run Automated Tests
```powershell
mvn clean test
```

### 2. Run the Full Refactoring Planner Pipeline
```powershell
mvn exec:java
```
Or specify a custom Java project directory:
```powershell
mvn exec:java -Dexec.args="path/to/your/java/project"
```

### 3. Run the Research Evaluation Analytics (Python)
```powershell
python evaluation/scripts/evaluate_refactoring.py
```
This generates charts and metrics inside `evaluation/results/`:
- `goal_comparison_barchart.png`: Grouped bar chart comparing priority scores across goals.
- `score_factors_breakdown.png`: Stacked bar chart of factor contributions.
- `ranking_shift_trajectory.png`: Slope graph of ranking position shifts.
- `evaluation_summary.md` and `evaluation_metrics.csv`.
