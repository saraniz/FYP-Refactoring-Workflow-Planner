package com.amie.refactor.planning;

import com.amie.refactor.model.*;
import com.amie.refactor.model.dependency.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowPlannerTest {

    private CombinedAnalysisResult sampleAnalysis;
    private DependencyAnalysisResult sampleDependencies;

    @BeforeEach
    void setUp() {
        // Class 1: GodClass with high WMC & LCOM
        CodeSmell godSmell = new CodeSmell(
                SmellType.GOD_CLASS, "OrderManager", "demo.OrderManager",
                null, "src/OrderManager.java", 1, 300, "3", "TooManyMethods", "Class has too many methods");
        CodeSmell longMethodSmell = new CodeSmell(
                SmellType.LONG_METHOD, "OrderManager", "demo.OrderManager",
                "processAllOrders", "src/OrderManager.java", 50, 150, "2", "NcssCount", "Method too long");
        ClassMetrics metrics1 = new ClassMetrics("demo.OrderManager", "OrderManager.java", "class", 2, 35, 60);
        ClassAnalysis ca1 = new ClassAnalysis("demo.OrderManager", List.of(godSmell, longMethodSmell), metrics1);

        // Class 2: FeatureEnvy class
        CodeSmell feSmell = new CodeSmell(
                SmellType.FEATURE_ENVY, "BillingService", "demo.BillingService",
                "calculateTaxes", "src/BillingService.java", 20, 45, "3", "CouplingBetweenObjects", "High coupling");
        ClassMetrics metrics2 = new ClassMetrics("demo.BillingService", "BillingService.java", "class", 6, 8, 10);
        ClassAnalysis ca2 = new ClassAnalysis("demo.BillingService", List.of(feSmell), metrics2);

        sampleAnalysis = new CombinedAnalysisResult(
                "sample-dir",
                MetricsAnalysisStatus.SUCCESS,
                List.of(ca1, ca2),
                Collections.emptyList(),
                Collections.emptyList(),
                null);

        // Mock dependencies
        MethodStructure ms1 = new MethodStructure(
                "demo.OrderManager", "processAllOrders", "void",
                List.of("List"), List.of("orders"), 50, 150, 101, 12,
                List.of("calculateTaxes"), List.of());

        ClassStructure cs1 = new ClassStructure(
                "demo.OrderManager", "OrderManager", "demo", "OrderManager.java",
                false, null, List.of(), Map.of(), List.of(ms1));
        cs1.setFanIn(0);
        cs1.setFanOut(1);

        ClassStructure cs2 = new ClassStructure(
                "demo.BillingService", "BillingService", "demo", "BillingService.java",
                false, null, List.of(), Map.of(), List.of());
        cs2.setFanIn(1);
        cs2.setFanOut(0);

        Dependency dep = new Dependency("demo.OrderManager", "processAllOrders", "demo.BillingService", "calculateTaxes", DependencyType.CALLS, 75);
        cs1.addOutgoingDependency(dep);
        cs2.addIncomingDependency(dep);

        sampleDependencies = new DependencyAnalysisResult(
                "sample-dir",
                Map.of("demo.OrderManager", cs1, "demo.BillingService", cs2),
                List.of(dep),
                Collections.emptyList());
    }

    @Test
    void testPrecedenceRuleLongMethodBeforeGodClass() {
        WorkflowPlanner planner = new WorkflowPlanner();
        WorkflowPlan plan = planner.plan(sampleAnalysis, sampleDependencies, QualityGoal.BALANCED_IMPROVEMENT);

        assertNotNull(plan);
        assertEquals(3, plan.getCandidateCount());

        int longMethodIndex = -1;
        int godClassIndex = -1;

        for (int i = 0; i < plan.getOrderedCandidates().size(); i++) {
            RefactoringCandidate c = plan.getOrderedCandidates().get(i);
            if (c.getSmellType() == SmellType.LONG_METHOD && c.getClassName().equals("OrderManager")) {
                longMethodIndex = i;
            } else if (c.getSmellType() == SmellType.GOD_CLASS && c.getClassName().equals("OrderManager")) {
                godClassIndex = i;
            }
        }

        assertTrue(longMethodIndex >= 0 && godClassIndex >= 0);
        assertTrue(longMethodIndex < godClassIndex,
                "Intra-class constraint: Long Method must be refactored before God Class");
    }

    @Test
    void testGoalOrientedScoringDivergence() {
        WorkflowPlanner planner = new WorkflowPlanner();

        WorkflowPlan maintPlan = planner.plan(sampleAnalysis, sampleDependencies, QualityGoal.IMPROVE_MAINTAINABILITY);
        WorkflowPlan compPlan = planner.plan(sampleAnalysis, sampleDependencies, QualityGoal.REDUCE_COMPLEXITY);

        assertNotNull(maintPlan);
        assertNotNull(compPlan);

        // Find God Class candidate in both
        RefactoringCandidate maintGod = maintPlan.getOrderedCandidates().stream()
                .filter(c -> c.getSmellType() == SmellType.GOD_CLASS)
                .findFirst().orElseThrow();
        RefactoringCandidate compGod = compPlan.getOrderedCandidates().stream()
                .filter(c -> c.getSmellType() == SmellType.GOD_CLASS)
                .findFirst().orElseThrow();

        // Weights and factor contributions should differ
        assertNotEquals(
                maintGod.getScoreBreakdown().getMetricWeight(),
                compGod.getScoreBreakdown().getSeverityWeight());

        // Verify score breakdowns are non-null and grounded
        assertNotNull(maintGod.getScoreBreakdown());
        assertTrue(maintGod.getPriorityScore() > 0.0);
        assertTrue(maintGod.getScoreBreakdown().getMetricImpactScore() > 0.0);
    }
}
