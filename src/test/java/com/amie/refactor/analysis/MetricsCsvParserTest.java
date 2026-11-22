package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassMetrics;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MetricsCsvParser}. These do not require CK Tool; they use small
 * fixture CSV files under {@code test-data/ck-metrics} with known values.
 */
class MetricsCsvParserTest {

    private static final Path FIXTURES = Path.of("test-data", "ck-metrics");

    private final MetricsCsvParser parser = new MetricsCsvParser();

    @Test
    void parsesValidCsvWithAllRequiredMetrics() throws Exception {
        List<ClassMetrics> metrics = parser.parse(FIXTURES.resolve("valid-class.csv"));

        assertEquals(3, metrics.size());

        ClassMetrics god = metrics.get(0);
        assertEquals("demo.GodClassExample", god.getClassName());
        assertEquals("class", god.getType());
        assertEquals(12, god.getCbo());
        assertEquals(20, god.getWmc());
        assertEquals(8, god.getLcom());
        assertTrue(god.hasAllMetrics());
    }

    @Test
    void associatesEachClassWithItsOwnMetricsIncludingFullyQualifiedNames() throws Exception {
        List<ClassMetrics> metrics = parser.parse(FIXTURES.resolve("valid-class.csv"));

        ClassMetrics longMethod = metrics.stream()
                .filter(m -> m.getClassName().equals("demo.LongMethodExample"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, longMethod.getCbo());
        assertEquals(3, longMethod.getWmc());
        assertEquals(0, longMethod.getLcom());

        ClassMetrics featureEnvy = metrics.stream()
                .filter(m -> m.getClassName().equals("demo.FeatureEnvyExample"))
                .findFirst()
                .orElseThrow();
        assertEquals(4, featureEnvy.getCbo());
    }

    @Test
    void handlesDifferentColumnOrdering() throws Exception {
        List<ClassMetrics> metrics = parser.parse(FIXTURES.resolve("reordered-class.csv"));

        assertEquals(2, metrics.size());
        ClassMetrics god = metrics.get(0);
        assertEquals("demo.GodClassExample", god.getClassName());
        assertEquals(12, god.getCbo());
        assertEquals(20, god.getWmc());
        assertEquals(8, god.getLcom());
    }

    @Test
    void handlesQuotedFields() throws Exception {
        List<ClassMetrics> metrics = parser.parse(FIXTURES.resolve("quoted-class.csv"));

        assertEquals(1, metrics.size());
        ClassMetrics demo = metrics.get(0);
        assertEquals("demo.Demo", demo.getClassName());
        assertEquals(3, demo.getCbo());
        // The quoted file column contained an embedded comma and must survive intact.
        assertTrue(demo.getSourceFile().contains("weird,dir"),
                "quoted field with embedded comma was not preserved: " + demo.getSourceFile());
    }

    @Test
    void rejectsEmptyFile() {
        CKToolException exception = assertThrows(CKToolException.class,
                () -> parser.parse(FIXTURES.resolve("empty.csv")));
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    void rejectsMissingRequiredColumns() {
        CKToolException exception = assertThrows(CKToolException.class,
                () -> parser.parse(FIXTURES.resolve("missing-columns.csv")));
        assertTrue(exception.getMessage().contains("missing required column"));
        assertTrue(exception.getMessage().contains("lcom"));
    }

    @Test
    void rejectsUnexpectedHeaders() {
        CKToolException exception = assertThrows(CKToolException.class,
                () -> parser.parse(FIXTURES.resolve("unexpected-headers.csv")));
        assertTrue(exception.getMessage().contains("missing required column"));
    }

    @Test
    void rejectsMalformedNumericValues() {
        CKToolException exception = assertThrows(CKToolException.class,
                () -> parser.parse(FIXTURES.resolve("malformed-numeric.csv")));
        assertTrue(exception.getMessage().contains("Malformed numeric value"));
        assertTrue(exception.getMessage().contains("cbo"));
    }

    @Test
    void treatsMissingMetricValueAsUnknownNotNullZero() throws Exception {
        List<ClassMetrics> metrics = parser.parse(FIXTURES.resolve("missing-values.csv"));

        ClassMetrics demo = metrics.get(0);
        assertEquals(3, demo.getCbo());
        assertNull(demo.getWmc(), "an absent value must be null, not fabricated as zero");
        assertEquals(1, demo.getLcom());
        assertFalse(demo.hasAllMetrics());
        assertTrue(demo.describeMissingMetrics().contains("wmc"));
    }

    @Test
    void rejectsDuplicateClassEntries() {
        CKToolException exception = assertThrows(CKToolException.class,
                () -> parser.parse(FIXTURES.resolve("duplicate-class.csv")));
        assertTrue(exception.getMessage().contains("Duplicate CK metrics entry"));
        assertTrue(exception.getMessage().contains("demo.Demo"));
    }

    @Test
    void reportsMissingFileWithClearMessage() {
        assertThrows(java.io.IOException.class,
                () -> parser.parse(FIXTURES.resolve("does-not-exist.csv")));
    }
}