# Research Decision Phases & Configurable Parameters Guide

This document catalogs the key research decision phases of the **Code-Aware Refactoring Workflow Planner**. For each phase, it provides the theoretical rationale, the **sensible default implementation** currently used in the system, where it is located in the codebase, and how you can tune or research it further for your final year thesis.

---

## Overview of Research Decision Phases

| Phase | Research Decision | Current Default Formulation | Java Source Location |
|:---|:---|:---|:---|
| **Phase 1** | Code Smell Severity Normalization (\(S_i\)) | PMD Priority 1–5 mapped to \([0.25, 1.00]\), boosted by extreme lines/methods | `PriorityScorer.java` (`computeSeverity`) |
| **Phase 2** | Software Metrics Normalization & Goal Formulation (\(M_i\)) | Saturation normalization over CBO (\(\le 12\)), WMC (\(\le 40\)), LCOM (\(\le 35\)) | `PriorityScorer.java`, `ScoringConfiguration.java` |
| **Phase 3** | Dependency Centrality & Ripple Impact (\(D_i\)) | Ripple potential: \(D_i = \min(1.0, \frac{2 \times \text{FanIn} + \text{FanOut}}{10})\) | `PriorityScorer.java` (`computeDependencyImpact`) |
| **Phase 4** | Estimated Refactoring Benefit Heuristics (\(B_i\)) | Fowler action benefit: God Class (0.90), Long Method (0.85), Feature Envy (0.75), Data Clumps (0.65) | `PriorityScorer.java` (`computeEstimatedBenefit`) |
| **Phase 5** | Goal-Oriented Weight Profiles (\(w_s, w_m, w_d, w_b\)) | Preset weights for Maintainability, Complexity, and Balanced goals | `GoalWeights.java` (`forGoal`) |
| **Phase 6** | Precedence & Ordering Constraints | Intra-class method-before-class, Parameter object before extraction, Callee-before-caller | `WorkflowPlanner.java`, `RefactoringDependencyGraph.java` |
| **Phase 7** | Grounded Code-Aware Explanation Rules | Template synthesis grounded in AST lines, CK metrics, caller/callee context | `ExplanationGenerator.java` |
| **Phase 8** | Benchmark Evaluation Protocol | 6 open-source benchmark projects, pre/post refactoring metrics, Python evaluation | `evaluate_refactoring.py`, `evaluation/` |

---

## Detailed Breakdown of Each Phase

### Phase 1: Code Smell Severity Normalization (\(S_i\))

- **Research Question:** How should raw detection output from PMD be quantified into a normalized severity score \(S_i \in [0.0, 1.0]\)?
- **Literature Context:** PMD assigns priority ratings (1 = High, 2 = Medium-High, 3 = Medium, 4 = Medium-Low, 5 = Low). However, raw priorities do not consider the degree of smell intensity (Fontana et al., 2015; Alkharabsheh et al., 2023).
- **Current Default Implementation:**
  - Base mapping: Priority 1 \(\to 1.00\), Priority 2 \(\to 0.85\), Priority 3 \(\to 0.65\), Priority 4 \(\to 0.45\), Priority 5 \(\to 0.25\).
  - Intensity modifier: If method length exceeds 50 lines (NCSS) or class methods exceed 15, \(S_i\) receives an additional \(+0.10\) boost (clamped to \(1.00\)).
- **How to Tune Later:** You can experiment with non-linear saturation curves or sigmoid transformations in [`PriorityScorer.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/planning/PriorityScorer.java#L52).

---

### Phase 2: Software Metrics Impact & Goal Synthesis (\(M_i\))

- **Research Question:** How should CK metrics (CBO, WMC, LCOM) be normalized and combined depending on the developer's selected quality goal?
- **Literature Context:** Chidamber & Kemerer (1994) defined CBO, WMC, and LCOM. High WMC indicates cognitive complexity; high CBO and LCOM represent high coupling and low cohesion, degrading maintainability (Briand et al., 2000).
- **Current Default Implementation:**
  - Normalization bounds defined in [`ScoringConfiguration.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/planning/ScoringConfiguration.java):
    - \(\text{CBO}_{\text{norm}} = \min(1.0, \text{CBO} / 12.0)\)
    - \(\text{WMC}_{\text{norm}} = \min(1.0, \text{WMC} / 40.0)\)
    - \(\text{LCOM}_{\text{norm}} = \min(1.0, \text{LCOM} / 35.0)\)
  - Goal Syntheses:
    - **Improve Maintainability:** \(M_i = 0.45 \times \text{CBO}_{\text{norm}} + 0.40 \times \text{LCOM}_{\text{norm}} + 0.15 \times \text{WMC}_{\text{norm}}\)
    - **Reduce Complexity:** \(M_i = 0.60 \times \text{WMC}_{\text{norm}} + 0.30 \times \text{MethodCC}_{\text{norm}} + 0.10 \times \text{CBO}_{\text{norm}}\)
    - **Balanced Improvement:** \(M_i = 0.40 \times \text{WMC}_{\text{norm}} + 0.30 \times \text{CBO}_{\text{norm}} + 0.30 \times \text{LCOM}_{\text{norm}}\)
- **How to Tune Later:** You can run statistical percentile distributions on large benchmark datasets (e.g. 75th or 90th percentile from Qualitas Corpus) to set the saturation thresholds empirically.

---

### Phase 3: Dependency Centrality & Ripple Impact (\(D_i\))

- **Research Question:** How do structural class dependencies influence refactoring risk and priority?
- **Literature Context:** Zhang et al. (2025) and Moonen (2013) demonstrated that code smells propagate through static dependencies. Classes with high Fan-In (many dependents) cause ripple effects if modified recklessly, whereas high Fan-Out means high sensitivity to external changes.
- **Current Default Implementation:**
  - Fan-in is weighted twice as heavily as Fan-out:
    \[
    D_i = \min\left(1.0, \frac{2 \times \text{FanIn} + 1 \times \text{FanOut}}{10.0}\right)
    \]
- **How to Tune Later:** In [`PriorityScorer.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/planning/PriorityScorer.java#L125), you can incorporate PageRank or betweenness centrality over the dependency graph.

---

### Phase 4: Estimated Refactoring Benefit Heuristics (\(B_i\))

- **Research Question:** How much architectural improvement does resolving a specific smell provide?
- **Literature Context:** Fowler (2018) refactoring catalog. Decomposing a God Class produces high modularization gains; decomposing Long Methods improves readability and localized testability.
- **Current Default Implementation:**
  - Base benefits:
    - `GOD_CLASS` \(\to 0.90\)
    - `LONG_METHOD` \(\to 0.85\)
    - `FEATURE_ENVY` \(\to 0.75\)
    - `DATA_CLUMPS` \(\to 0.65\)
  - Goal alignment: \(+0.08\) bonus if the smell directly targets the active goal (e.g. Long Method under Reduce Complexity).
- **How to Tune Later:** You can survey developers or measure historical before-and-after commit metrics to derive empirical benefit multipliers.

---

### Phase 5: Goal-Oriented Weight Profiles (\(w_s, w_m, w_d, w_b\))

- **Research Question:** What relative weights should be assigned to the four factors for each developer goal?
- **Current Default Implementation in [`GoalWeights.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/planning/GoalWeights.java):**
  - **Improve Maintainability:** \(w_s = 0.20, w_m = 0.35, w_d = 0.25, w_b = 0.20\)
  - **Reduce Complexity:** \(w_s = 0.30, w_m = 0.35, w_d = 0.15, w_b = 0.20\)
  - **Balanced Improvement:** \(w_s = 0.25, w_m = 0.25, w_d = 0.25, w_b = 0.25\)
- **How to Tune Later:** In your thesis evaluation, perform **sensitivity analysis** by varying weights (e.g. \(\pm 10\%\)) to show supervisors how robust the resulting rankings are.

---

### Phase 6: Precedence & Ordering Constraints (DAG)

- **Research Question:** Which refactorings must strictly precede others, regardless of raw priority scores?
- **Current Default Rules in [`WorkflowPlanner.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/planning/WorkflowPlanner.java#L85):**
  1. **Intra-class method before class:** Long Method extractions must precede God Class decomposition in the same class (ensuring cohesive methods exist before extracting new classes).
  2. **Data Clump parameter object before extraction:** Introducing a parameter object must precede downstream method extractions in the same class.
  3. **Callee before Caller:** If Class A calls Class B, Class B's internal refactorings must be sequenced before Class A's refactorings to stabilize contracts.
  4. **Cycle Resolution:** If a circular precedence is introduced, the cycle edge is broken with an explanatory warning.
- **Ordering Algorithm:** Kahn's algorithm with a **Max-Priority Queue** (`RefactoringDependencyGraph.java`). Candidates with 0 unsatisfied prerequisites are scheduled first, ordered by highest priority score.

---

### Phase 7: Grounded Explanation Rules

- **Research Question:** How to generate trustworthy, verifiable explanations that reference actual code rather than hallucinated text?
- **Implementation in [`ExplanationGenerator.java`](file:///c:/amand/Amie/Code%20Aware%20Refactoring%20Planner/refactoring-workflow-planner/src/main/java/com/amie/refactor/explanation/ExplanationGenerator.java):**
  - Produces structured sections: **What & Where**, **Why Recommended**, **Why Prioritized**, **Why in this Sequence**, and **Actionable Steps**.
  - All claims cite verified facts: line numbers, PMD rule name, CK metric values (CBO, WMC, LCOM), called methods, and prerequisite IDs.

---

### Phase 8: Benchmark Evaluation Protocol

- **Objective 5 Evaluation Plan:**
  - Select 6 open-source Java benchmark projects (e.g. *Apache Commons Lang, JFreeChart, Joda-Time, Gson, JUnit 4, Google Guava*).
  - Run the planner across all 3 goals.
  - Run `python evaluation/scripts/evaluate_refactoring.py` to produce comparative charts and correlation statistics.
  - Manually carry out top-3 refactorings in a sample project, re-run CK Tool, and verify that WMC/CBO/LCOM decrease as predicted!
