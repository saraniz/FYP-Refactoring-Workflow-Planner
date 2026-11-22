package com.amie.refactor.analysis;

import com.amie.refactor.model.ClassMetrics;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses the class-level CSV file produced by CK Tool 0.7.0 into {@link ClassMetrics}.
 *
 * <p>Columns are located by <em>name</em> from the CSV header, so column order does not
 * matter and extra CK columns (for example {@code cboModified}, {@code nosi}, ...) are
 * ignored. Parsing uses Apache Commons CSV - the same library CK uses to write the file -
 * so quoted fields, embedded commas and escapes are handled without a naive comma split.</p>
 *
 * <p>CK 0.7.0 {@code class.csv} header:</p>
 * <pre>
 *   file,class,type,cbo,cboModified,fanin,fanout,wmc,dit,noc,rfc,lcom,lcom*,tcc,lcc,...
 * </pre>
 * <p>Only {@code class}, {@code cbo}, {@code wmc} and {@code lcom} are required.</p>
 *
 * <p>Structural problems (missing file, empty file, no header, missing required columns,
 * a record without a class name, or a duplicate class entry) throw {@link CKToolException}
 * because they make the CSV unusable or ambiguous. A cell that is empty yields a
 * {@code null} metric ("not present in the CK output", never a fabricated zero); a cell
 * that is present but not an integer is treated as corrupted data and throws.</p>
 */
public class MetricsCsvParser {

    static final String CLASS_COLUMN = "class";
    static final String CBO_COLUMN = "cbo";
    static final String WMC_COLUMN = "wmc";
    static final String LCOM_COLUMN = "lcom";
    static final String FILE_COLUMN = "file";
    static final String TYPE_COLUMN = "type";

    /**
     * Parses {@code class.csv}.
     *
     * @param classCsvPath path to the CK Tool class-level CSV file
     * @return one {@link ClassMetrics} per class, in file order
     * @throws IOException     if the file cannot be read
     * @throws CKToolException if the file is empty, lacks required columns, contains a
     *                         malformed numeric value or a duplicate class entry
     */
    public List<ClassMetrics> parse(Path classCsvPath) throws IOException {

        if (classCsvPath == null) {
            throw new IllegalArgumentException("CK metrics CSV path must not be null");
        }
        if (!Files.isRegularFile(classCsvPath)) {
            throw new IOException("CK metrics CSV file not found: " + classCsvPath.toAbsolutePath());
        }
        if (Files.size(classCsvPath) == 0L) {
            throw new CKToolException("CK metrics CSV file is empty: " + classCsvPath.toAbsolutePath());
        }

        List<ClassMetrics> metrics = new ArrayList<>();
        Set<String> seenClasses = new LinkedHashSet<>();

        try (Reader reader = Files.newBufferedReader(classCsvPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .get()
                     .parse(reader)) {

            Map<String, Integer> header = normalisedHeader(parser, classCsvPath);

            long rowNumber = 1L; // row 1 is the header
            for (CSVRecord record : parser) {
                rowNumber++;
                metrics.add(parseRecord(record, header, rowNumber, classCsvPath, seenClasses));
            }
        }

        return metrics;
    }

    /**
     * Builds a case-insensitive, trimmed view of the header (column name to index) and
     * verifies that every required column is present.
     */
    private Map<String, Integer> normalisedHeader(CSVParser parser, Path file) {
        Map<String, Integer> rawHeader = parser.getHeaderMap();
        if (rawHeader == null || rawHeader.isEmpty()) {
            throw new CKToolException("CK metrics CSV file does not contain a header row: "
                    + file.toAbsolutePath());
        }

        Map<String, Integer> header = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : rawHeader.entrySet()) {
            header.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue());
        }

        List<String> missing = new ArrayList<>();
        for (String required : List.of(CLASS_COLUMN, CBO_COLUMN, WMC_COLUMN, LCOM_COLUMN)) {
            if (!header.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new CKToolException("CK metrics CSV file is missing required column(s): "
                    + String.join(", ", missing) + ". Available columns: " + header.keySet());
        }
        return header;
    }

    private ClassMetrics parseRecord(
            CSVRecord record,
            Map<String, Integer> header,
            long rowNumber,
            Path file,
            Set<String> seenClasses) {

        String className = value(record, header, CLASS_COLUMN);
        if (className == null || className.isBlank()) {
            throw new CKToolException("Row " + rowNumber + " of " + file.getFileName()
                    + " has an empty '" + CLASS_COLUMN + "' value");
        }
        className = className.trim();

        if (!seenClasses.add(className)) {
            throw new CKToolException("Duplicate CK metrics entry for class '" + className
                    + "' at row " + rowNumber + " of " + file.getFileName());
        }

        Integer cbo = parseMetric(record, header, CBO_COLUMN, rowNumber, file);
        Integer wmc = parseMetric(record, header, WMC_COLUMN, rowNumber, file);
        Integer lcom = parseMetric(record, header, LCOM_COLUMN, rowNumber, file);

        return new ClassMetrics(
                className,
                value(record, header, FILE_COLUMN),
                value(record, header, TYPE_COLUMN),
                cbo,
                wmc,
                lcom);
    }

    /**
     * Reads an integer metric. An empty cell yields {@code null} (the value was absent and
     * is reported as "unknown", never fabricated as zero); a non-numeric cell is a hard error.
     */
    private Integer parseMetric(
            CSVRecord record,
            Map<String, Integer> header,
            String column,
            long rowNumber,
            Path file) {

        String raw = value(record, header, column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new CKToolException("Malformed numeric value in column '" + column
                    + "' at row " + rowNumber + " of " + file.getFileName() + ": '" + raw + "'");
        }
    }

    private static String value(CSVRecord record, Map<String, Integer> header, String column) {
        Integer index = header.get(column);
        if (index == null || index >= record.size()) {
            return null;
        }
        return record.get(index);
    }
}