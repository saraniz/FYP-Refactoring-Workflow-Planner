package com.amie.refactor.explanation;

import com.amie.refactor.model.*;
import com.amie.refactor.planning.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExplanationGeneratorTest {

    @Test
    void testExplanationGenerationGroundedInRealFacts() {
        CodeSmell smell = new CodeSmell(
                SmellType.LONG_METHOD, "OrderService", "demo.OrderService",
                "processOrder", "src/demo/OrderService.java", 45, 120, "2", "NcssCount", "NCSS count of 75");
        ClassMetrics metrics = new ClassMetrics("demo.OrderService", "OrderService.java", "class", 3, 28, 15);

        RefactoringCandidate candidate = new RefactoringCandidate(
                "RC-01", smell, RefactoringAction.EXTRACT_METHOD, metrics, null, null);

        PriorityScorer scorer = new PriorityScorer();
        ScoreBreakdown breakdown = scorer.calculateScore(
                candidate, QualityGoal.IMPROVE_MAINTAINABILITY, GoalWeights.forGoal(QualityGoal.IMPROVE_MAINTAINABILITY));
        candidate.setScoreBreakdown(breakdown);

        WorkflowPlan plan = new WorkflowPlan(
                "sample-dir",
                QualityGoal.IMPROVE_MAINTAINABILITY,
                GoalWeights.forGoal(QualityGoal.IMPROVE_MAINTAINABILITY),
                List.of(candidate),
                0,
                List.of());

        ExplanationGenerator generator = new ExplanationGenerator();
        List<WorkflowStep> steps = generator.generateWorkflowSteps(plan, null);

        assertEquals(1, steps.size());
        WorkflowStep step1 = steps.get(0);
        assertEquals(1, step1.getStepNumber());
        assertTrue(step1.getTitle().contains("Extract Method"));

        StepExplanation exp = step1.getExplanation();
        assertNotNull(exp);

        // Grounding checks
        assertTrue(exp.getWhatAndWhere().contains("OrderService.processOrder()"));
        assertTrue(exp.getWhatAndWhere().contains("45"));
        assertTrue(exp.getWhatAndWhere().contains("120"));
        assertTrue(exp.getWhatAndWhere().contains("LONG_METHOD"));

        assertTrue(exp.getWhyRecommended().contains("WMC=28"));
        assertTrue(exp.getWhyRecommended().contains("CBO=3"));

        assertTrue(exp.getWhyPrioritized().contains("Improve Maintainability"));
        assertTrue(exp.getWhyPrioritized().contains("Priority Score"));

        assertTrue(exp.getWhyInThisSequence().contains("Step 1"));
        assertFalse(exp.getActionableGuidance().isEmpty());
    }
}
