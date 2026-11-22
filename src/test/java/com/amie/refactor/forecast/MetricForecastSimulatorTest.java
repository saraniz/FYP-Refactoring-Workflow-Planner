package com.amie.refactor.forecast;

import com.amie.refactor.model.ClassMetrics;
import com.amie.refactor.model.CodeSmell;
import com.amie.refactor.model.SmellType;
import com.amie.refactor.model.dependency.ClassStructure;
import com.amie.refactor.model.dependency.Dependency;
import com.amie.refactor.model.dependency.DependencyType;
import com.amie.refactor.model.dependency.MethodStructure;
import com.amie.refactor.planning.RefactoringAction;
import com.amie.refactor.planning.RefactoringCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MetricForecastSimulatorTest {

    private MetricForecastSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new MetricForecastSimulator();
    }

    @Test
    @DisplayName("Should predict significant WMC and LCOM reduction for God Class")
    void testGodClassForecast() {
        CodeSmell smell = new CodeSmell(
                SmellType.GOD_CLASS, "BigGodClass", "demo.BigGodClass", "N/A", "test/path", 10, 200, "2", "TooManyMethods", "Class too big");
        ClassMetrics metrics = new ClassMetrics("demo.BigGodClass", "test/path", "class", 4, 30, 85);

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-01", smell, RefactoringAction.EXTRACT_CLASS, metrics, null, null);

        MetricForecast forecast = simulator.simulate(candidate);

        assertNotNull(forecast);
        assertEquals(30, forecast.getWmcBefore());
        assertTrue(forecast.getWmcAfter() < forecast.getWmcBefore(), "WMC after should be reduced");
        assertEquals(85, forecast.getLcomBefore());
        assertTrue(forecast.getLcomAfter() < forecast.getLcomBefore(), "LCOM after should be reduced");
        assertTrue(forecast.getWmcReductionPct() > 40.0, "WMC reduction percentage should exceed 40%");
        assertTrue(forecast.getForecastSummary().contains("Extract Class"));
    }

    @Test
    @DisplayName("Should predict method length reduction for Long Method")
    void testLongMethodForecast() {
        CodeSmell smell = new CodeSmell(
                SmellType.LONG_METHOD, "OrderService", "demo.OrderService", "processAll", "test/path", 15, 80, "2", "NcssCount", "Method too long");
        ClassMetrics metrics = new ClassMetrics("demo.OrderService", "test/path", "class", 2, 12, 10);
        MethodStructure ms = new MethodStructure(
                "OrderService", "processAll", "void", List.of("String", "int"), List.of("id", "qty"), 15, 80, 65, 9,
                Collections.emptyList(), Collections.emptyList());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-02", smell, RefactoringAction.EXTRACT_METHOD, metrics, ms, null);

        MetricForecast forecast = simulator.simulate(candidate);

        assertNotNull(forecast);
        assertEquals(66, forecast.getLocBefore());
        assertTrue(forecast.getLocAfter() < forecast.getLocBefore(), "LOC after should be smaller for orchestrator method");
        assertTrue(forecast.getForecastSummary().contains("Extract Method"));
    }

    @Test
    @DisplayName("Should predict CBO decoupling for Feature Envy")
    void testFeatureEnvyForecast() {
        CodeSmell smell = new CodeSmell(
                SmellType.FEATURE_ENVY, "Reporter", "demo.Reporter", "buildReport", "test/path", 5, 25, "3", "CouplingBetweenObjects", "Feature envy");
        ClassMetrics metrics = new ClassMetrics("demo.Reporter", "test/path", "class", 5, 8, 20);

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-03", smell, RefactoringAction.MOVE_METHOD, metrics, null, null);

        MetricForecast forecast = simulator.simulate(candidate);

        assertNotNull(forecast);
        assertEquals(5, forecast.getCboBefore());
        assertEquals(4, forecast.getCboAfter(), "CBO should decrement by 1 after moving envious method");
    }

    @Test
    @DisplayName("Should predict parameter count consolidation for Data Clumps")
    void testDataClumpsForecast() {
        CodeSmell smell = new CodeSmell(
                SmellType.DATA_CLUMPS, "Shipping", "demo.Shipping", "shipItem", "test/path", 10, 20, "3", "ExcessiveParameterList", "Parameter clump");
        MethodStructure ms = new MethodStructure(
                "Shipping", "shipItem", "void",
                List.of("String", "String", "String", "String", "int"),
                List.of("street", "city", "state", "zip", "weight"),
                10, 20, 11, 2, Collections.emptyList(), Collections.emptyList());

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-04", smell, RefactoringAction.INTRODUCE_PARAMETER_OBJECT, null, ms, null);

        MetricForecast forecast = simulator.simulate(candidate);

        assertNotNull(forecast);
        assertEquals(5, forecast.getParamCountBefore());
        assertEquals(1, forecast.getParamCountAfter(), "Parameters should consolidate to 1 unified object");
    }

    @Test
    @DisplayName("Should classify leaf class with 0 Fan-In as Quick Win")
    void testQuickWinBlastRadius() {
        ClassStructure cs = new ClassStructure(
                "demo.LeafHelper", "LeafHelper", "demo", "test/path", false, "Object",
                Collections.emptyList(), Map.of(), Collections.emptyList());
        cs.setFanIn(0);
        cs.setFanOut(1);

        CodeSmell smell = new CodeSmell(
                SmellType.LONG_METHOD, "LeafHelper", "demo.LeafHelper", "helperMethod", "test/path", 1, 30, "3", "NcssCount", "Method too long");
        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-05", smell, RefactoringAction.EXTRACT_METHOD, null, null, cs);

        BlastRadiusInfo info = simulator.evaluateBlastRadius(candidate);

        assertNotNull(info);
        assertEquals(RiskCategory.QUICK_WIN, info.getRiskCategory());
        assertTrue(info.getRiskBadge().contains("Quick Win"));
        assertTrue(info.getAffectedCallers().isEmpty());
    }

    @Test
    @DisplayName("Should classify core service with callers as Moderate or High Ripple Risk")
    void testHighRippleRiskBlastRadius() {
        ClassStructure cs = new ClassStructure(
                "demo.PaymentProcessor", "PaymentProcessor", "demo", "test/path", false, "Object",
                Collections.emptyList(), Map.of(), Collections.emptyList());
        cs.setFanIn(5);
        cs.setFanOut(3);

        cs.addIncomingDependency(new Dependency("demo.OrderService", "executeOrder", "demo.PaymentProcessor", "processCreditCard", DependencyType.CALLS, 25));
        cs.addIncomingDependency(new Dependency("demo.BillingController", "billUser", "demo.PaymentProcessor", "processCreditCard", DependencyType.CALLS, 40));

        CodeSmell smell = new CodeSmell(
                SmellType.GOD_CLASS, "PaymentProcessor", "demo.PaymentProcessor", "N/A", "test/path", 1, 100, "2", "TooManyMethods", "Class too big");
        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-06", smell, RefactoringAction.EXTRACT_CLASS, null, null, cs);

        BlastRadiusInfo info = simulator.evaluateBlastRadius(candidate);

        assertNotNull(info);
        assertEquals(RiskCategory.MODERATE_RIPPLE, info.getRiskCategory());
        assertEquals(2, info.getAffectedCallers().size());
        assertTrue(info.getAffectedCallers().contains("demo.OrderService"));
        assertTrue(info.getAffectedCallers().contains("demo.BillingController"));
    }
}
