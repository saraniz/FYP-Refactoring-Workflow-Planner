package com.amie.refactor.diff;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for producing standard unified diff output between two code snippets.
 */
public class DiffUtils {

    /**
     * Generates a standard unified diff string between beforeCode and afterCode.
     */
    public static String generateUnifiedDiff(String fileName, String beforeCode, String afterCode) {
        String[] beforeLines = beforeCode.split("\\r?\\n");
        String[] afterLines = afterCode.split("\\r?\\n");

        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(fileName).append("\n");
        diff.append("+++ b/").append(fileName).append("\n");
        diff.append("@@ -1,").append(beforeLines.length).append(" +1,").append(afterLines.length).append(" @@\n");

        // Simple Myers/LCS line diff approximation for unified presentation
        int max = Math.max(beforeLines.length, afterLines.length);
        int bIdx = 0;
        int aIdx = 0;

        while (bIdx < beforeLines.length || aIdx < afterLines.length) {
            if (bIdx < beforeLines.length && aIdx < afterLines.length) {
                if (beforeLines[bIdx].equals(afterLines[aIdx])) {
                    diff.append(" ").append(beforeLines[bIdx]).append("\n");
                    bIdx++;
                    aIdx++;
                } else {
                    // Check if line was replaced or removed
                    diff.append("-").append(beforeLines[bIdx]).append("\n");
                    diff.append("+").append(afterLines[aIdx]).append("\n");
                    bIdx++;
                    aIdx++;
                }
            } else if (bIdx < beforeLines.length) {
                diff.append("-").append(beforeLines[bIdx]).append("\n");
                bIdx++;
            } else {
                diff.append("+").append(afterLines[aIdx]).append("\n");
                aIdx++;
            }
        }

        return diff.toString();
    }
}
