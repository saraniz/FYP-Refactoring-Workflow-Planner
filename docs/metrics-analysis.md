# Software Metrics Analysis Module (Part A — CK Tool)

This document describes the Software Metrics Analysis module and how it integrates with
the existing PMD code-smell detection module.

## 1. Purpose

The module runs the **CK Tool** against a selected Java source directory, parses its
class-level CSV output, and exposes three class-level metrics:

| Metric | Meaning (CK Tool 0.7.0) |
|--------|-------------------------|
| **CBO**  | Coupling Between Objects — number of dependencies the class has on other types (CK ignores dependencies on `java.lang.*` etc.). |
| **WMC**  | Weighted Methods per Class / McCabe complexity — number of branch instructions in the class. |
| **LCOM** | Lack of Cohesion of Methods — CK's first-version LCOM (`|P| - |Q|`, clamped to `>= 0`). |

The values are **exactly** as reported by CK Tool. They are supporting evidence, not proof
that a class must be refactored, and the module applies **no thresholds**.

## 2. CK Tool version and trusted source

- **Version:** CK Tool `0.7.0` (the latest release on Maven Central as of this work).
- **Artifact:** `com.github.mauricioaniche:ck:0.7.0`, executable
  `ck-0.7.0-jar-with-dependencies.jar`.
- **Source:** Maven Central —
  `https://repo1.maven.org/maven2/com/github/mauricioaniche/ck/0.7.0/`
  (upstream project: `https://github.com/mauricioaniche/ck`, Apache-2.0).

The executable "jar-with-dependencies" is published by Maven Central with a `.sha1`
checksum.

### Getting the JAR (reproducible, checksum-verified)

```powershell
# From the project root. Downloads to tools/ and verifies the Maven Central SHA1.
powershell -ExecutionPolicy Bypass -File tools/fetch-ck.ps1
# Optional explicit version:
powershell -ExecutionPolicy Bypass -File tools/fetch-ck.ps1 -Version 0.7.0
```

The JAR is **not** committed (see `.gitignore`); fetch it locally as above.

## 3. Compatibility

- **CK Tool 0.7.0** is built with JDT compliance **Java 11** (it parses sources up to the
  Java 11 language level). It runs on a modern JDK; it has been verified in this project
  running on **JDK 26** (this project's `maven.compiler.release`).
- The application itself requires the JDK used by the project (JDK 26 in this repository).
- CK parses source code only; no compiled classes are required.

## 4. Configuration

The CK Tool JAR is resolved, in order:

1. JVM system property `-Dck.jar=<path>`
2. Environment variable `CK_JAR=<path>`
3. Default: `tools/ck-0.7.0-jar-with-dependencies.jar`

The CSV output directory defaults to `results/ck-metrics` (project-relative, git-ignored).
`CKToolRunner` creates it if missing and never deletes unrelated files.

## 5. Selecting the target project

Pass the target's Java **source directory** (or project directory) to the analyzer. The
existing `ProjectScanner` is used to verify that Java sources exist and to define a
compatible source scope shared with the PMD module. In `Main` the sample project
`sample-projects/smell-test-project1` is used, with sources at
`sample-projects/smell-test-project1/src/main/java`.

## 6. Running the metrics analysis

End-to-end (PMD + metrics + combined view):

```powershell
# Run the application main class with the CK JAR configured.
java -Dck.jar=tools/ck-0.7.0-jar-with-dependencies.jar `
     -cp "target/classes;$HOME/.m2/repository/org/apache/commons/commons-csv/1.14.1/commons-csv-1.14.1.jar" `
     com.amie.refactor.Main
```

Programmatically:

```java
MetricsAnalysisResult result = new MetricsAnalyzer().analyze(
        Path.of("sample-projects/smell-test-project1/src/main/java"));
```

## 7. How CK Tool is executed

CK Tool 0.7.0 command line (see its README):

```
java -jar ck.jar <projectDir> <useJars:true|false> <maxFilesPerPartition:0=auto> <variablesAndFields:true|false> <outputDir>
```

`CKToolRunner` builds exactly this command. Two 0.7.0 behaviours are handled explicitly:

1. The output file names are built by **string concatenation** (`outputDir + "class.csv"`),
   so the output-directory argument is passed **with a trailing path separator**.
2. `variable.csv`/`field.csv` are only produced when variable/field metrics are enabled;
   the runner passes `false`, so only `class.csv` and `method.csv` are written.

The process is executed through the `ProcessExecutor` seam, which captures stdout/stderr
and enforces a configurable timeout (default 5 minutes). Non-zero exit codes, timeouts and
missing output all raise descriptive `CKToolException`s. Failures never delete or overwrite
existing files.

## 8. Where CSV output is stored

CK writes its CSV files into the configured output directory (default
`results/ck-metrics/`). The required file is `class.csv`.

## 9. How CSV data is parsed and matched to classes

`MetricsCsvParser` reads `class.csv` with **Apache Commons CSV** (the same library CK uses
to write it), locating columns **by header name** (case-insensitive), so column order does
not matter. The relevant columns are `class`, `cbo`, `wmc`, `lcom` (plus optional `file`,
`type`). The CK `class` column holds the type's **binary name** (`demo.OrderService`, or
`demo.Outer$Inner` for nested types), which is the fully qualified name for top-level
classes.

Parsing rules:

- Empty file / missing file / no header / missing required columns / a row without a class
  name / a duplicate class entry → `CKToolException` (structural problems are reported).
- An **empty** metric cell → `null` ("not present in the CK output"), never a fabricated
  zero.
- A **non-numeric** metric cell → `CKToolException` (corrupted data fails loudly).

`MetricsAnalyzer` then:

- returns **FAILED** with an explanatory error if the target is invalid, has no Java
  sources, or the tool/CSV fails;
- returns **PARTIAL** when classes have missing metric values (warnings list each one);
- returns **SUCCESS** when every class carries all required metrics.

## 10. How metrics are combined with PMD findings

`SmellMetricsIntegrator.combine(projectDirectory, smells, metricsResult)` produces a
`CombinedAnalysisResult` containing one `ClassAnalysis` per class known to **either** tool:

- matching is by **fully qualified class name** (PMD `package` + `class`; CK `class`
  column), never simple names alone — classes sharing a simple name in different packages
  are kept separate;
- a class with a smell but no metrics → kept, `metrics == null`, with a warning;
- a class with metrics but no smell → kept, empty smell list (the normal, clean case);
- duplicate CK entries for one class → first wins, with a warning;
- the original PMD `CodeSmell` objects (and their source locations) are carried through
  unchanged.

`PmdResultParser` was extended (minimally) to also expose PMD's `package` attribute so the
fully qualified class name is available for matching. No PMD rule or detection behaviour
was changed.

## 11. Running the tests

```powershell
# Unit tests only (CK integration tests are skipped automatically).
mvn test

# Full suite including the real CK Tool integration tests.
mvn test -Dck.jar=tools/ck-0.7.0-jar-with-dependencies.jar

# Clean build + tests.
mvn clean test
```

Test classes:

| Test | Runs without CK? | Covers |
|------|------------------|--------|
| `MetricsCsvParserTest` | yes | valid/reordered/quoted CSV, empty, missing columns, unexpected headers, malformed numbers, missing values, duplicates |
| `CKToolRunnerTest` | yes | command construction, missing JAR, invalid path, non-zero exit, timeout, missing output, JAR resolution |
| `MetricsAnalyzerTest` | yes | orchestration, invalid/empty target, success, partial, tool failure |
| `SmellMetricsIntegratorTest` | yes | smell+metrics, metrics-only, smell-only, same simple name in different packages, duplicates, no mis-attribution, PMD preservation |
| `CKToolIntegrationTest` | **no** (`-Dck.jar`) | real CK run on the sample project; combined with committed PMD findings |

CSV fixtures live in `test-data/ck-metrics/`.

## 12. Known limitations

- CK Tool 0.7.0 parses to the **Java 11** language level; sources using newer syntax may be
  partially or not parsed by CK.
- CK reports the type's **binary name** while PMD reports the package and class separately.
  Matching normalises `$` to `.` for nested types, but exotic/nested/anonymous naming can
  still leave a class unmatched — such cases are reported as warnings, never mis-attributed.
- LCOM is CK's non-negative first-version metric; treat it as supporting evidence.
- Metrics can be produced for local/anonymous/auxiliary types that PMD never reports (and
  vice-versa); both directions are preserved and surfaced.
- No thresholds or prioritisation are applied here — that remains the planner's concern.
- This module implements **Part A only**; JavaParser-based dependency analysis (Part B) is
  out of scope.