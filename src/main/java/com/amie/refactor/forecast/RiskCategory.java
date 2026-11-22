package com.amie.refactor.forecast;

/**
 * Categorizes the ripple impact and implementation effort of a refactoring candidate
 * based on AST call-graph Fan-In and Fan-Out.
 */
public enum RiskCategory {
    QUICK_WIN("Quick Win", "🟢", "#16a34a", "Zero external callers affected; safe to refactor immediately with isolated impact."),
    LOW_RISK("Low Risk", "🟡", "#ca8a04", "Limited caller dependencies; low ripple probability."),
    MODERATE_RIPPLE("Moderate Ripple Risk", "🟠", "#ea580c", "Multiple dependent callers detected; requires regression verification of caller contracts."),
    HIGH_RIPPLE_RISK("High Ripple Risk", "🔴", "#dc2626", "Heavily depended upon across the system; high blast radius requiring careful API compatibility.");

    private final String displayName;
    private final String emoji;
    private final String colorHex;
    private final String description;

    RiskCategory(String displayName, String emoji, String colorHex, String description) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.colorHex = colorHex;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeText() {
        return emoji + " " + displayName;
    }
}
