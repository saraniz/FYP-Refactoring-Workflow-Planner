# RefactorFlow — IntelliJ IDEA Extension

**RefactorFlow** is the native IntelliJ IDEA plugin for the *Code-Aware Refactoring Workflow Planner with Goal-Oriented Prioritization and Explainable Step Guidance for Java Systems*.

It provides an interactive, dockable Tool Window on IntelliJ IDEA's right sidebar that transforms static analysis outputs into actionable, prioritized, and explainable refactoring workflows with native side-by-side Diff previews.

---

## Key Features in IntelliJ IDEA

1. **Dockable Tool Window (`RefactorFlow`)**:
   - Registered on the right sidebar with IntelliJ's native `AllIcons.Actions.RefactoringBulb`.
   - Accessible anytime during Java software development.

2. **Goal Switcher Dropdown**:
   - Switch between **Improve Maintainability**, **Reduce Complexity**, and **Balanced Improvement**.
   - Dynamically re-orders refactoring steps according to objective weights.

3. **Step Cards & Grounded Explanations**:
   - Displays smell badges (`GOD_CLASS`, `LONG_METHOD`, `FEATURE_ENVY`, `DATA_CLUMPS`).
   - Displays computed Priority Score (\(P_i \in [0, 1]\)).
   - Shows sequence precedence rationale (DAG topological dependency constraint reasons).
   - Provides concrete, bulleted actionable steps.

4. **Native Side-by-Side Code Diff Viewer**:
   - Clicking **"Compare Diff"** opens IntelliJ's native `DiffManager` comparing original smelly code with the proposed refactored structure.

5. **Jump to Code**:
   - Clicking **"Jump to Code"** leverages `OpenFileDescriptor` to immediately navigate to the exact file and violation line number in the IntelliJ active editor.

---

## How to Open and Run in IntelliJ IDEA

### Method 1: Open Directly as a Gradle Project (Recommended)
1. Open IntelliJ IDEA.
2. Select **File -> Open...** and navigate to:
   ```
   c:\amand\Amie\Code Aware Refactoring Planner\intellij-plugin
   ```
3. IntelliJ IDEA will automatically recognize `build.gradle.kts` and sync Gradle dependencies via the JetBrains IntelliJ Platform Plugin.
4. Open the **Gradle tool window** on the right sidebar:
   - Navigate to `Tasks -> intellij platform -> runIde`.
   - Double-click `runIde`.
5. A sandbox instance of IntelliJ IDEA will launch with the **RefactorFlow** plugin pre-installed.
6. In the sandbox IntelliJ window, click the **RefactorFlow** tab on the right sidebar to view and interact with the workflow planner!

### Method 2: Build the Plugin `.zip` / `.jar` for Installation
To build the installable plugin distribution package:
1. In the Gradle tool window or terminal inside `intellij-plugin`:
   ```bash
   ./gradlew buildPlugin
   ```
2. The built distribution file will be generated in:
   ```
   intellij-plugin/build/distributions/intellij-plugin-1.0.0.zip
   ```
3. In any standard IntelliJ IDEA:
   - Go to **Settings / Preferences -> Plugins -> ⚙️ (Gear Icon) -> Install Plugin from Disk...**
   - Select the `.zip` file.
   - Restart IntelliJ IDEA.

---

## Architecture of the Plugin

```
intellij-plugin/
├── build.gradle.kts                 # JetBrains IntelliJ Platform Gradle configuration
├── src/main/
│   ├── resources/
│   │   └── META-INF/
│   │       └── plugin.xml          # ToolWindow registration & plugin metadata
│   └── java/com/amie/refactor/plugin/
│       ├── RefactorFlowToolWindowFactory.java # ToolWindow lifecycle & container factory
│       ├── RefactorFlowPanel.java             # Interactive Swing / JBUI interface & JSON parser
│       └── DiffViewerLauncher.java            # Bridge to IntelliJ DiffManager (native diff)
└── README.md                        # Setup and demonstration instructions
```
