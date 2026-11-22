package com.amie.refactor.forecast;

/**
 * Models simulated "What-If" metric forecasts, predicting the quantitative
 * impact of applying a specific refactoring action on CK metrics.
 */
public class MetricForecast {

    private final int wmcBefore;
    private final int wmcAfter;
    private final int lcomBefore;
    private final int lcomAfter;
    private final int cboBefore;
    private final int cboAfter;
    private final int locBefore;
    private final int locAfter;
    private final int paramCountBefore;
    private final int paramCountAfter;
    private final double wmcReductionPct;
    private final double lcomReductionPct;
    private final String forecastSummary;

    public MetricForecast(
            int wmcBefore,
            int wmcAfter,
            int lcomBefore,
            int lcomAfter,
            int cboBefore,
            int cboAfter,
            int locBefore,
            int locAfter,
            int paramCountBefore,
            int paramCountAfter,
            String forecastSummary) {

        this.wmcBefore = wmcBefore;
        this.wmcAfter = wmcAfter;
        this.lcomBefore = lcomBefore;
        this.lcomAfter = lcomAfter;
        this.cboBefore = cboBefore;
        this.cboAfter = cboAfter;
        this.locBefore = locBefore;
        this.locAfter = locAfter;
        this.paramCountBefore = paramCountBefore;
        this.paramCountAfter = paramCountAfter;

        this.wmcReductionPct = calculatePctReduction(wmcBefore, wmcAfter);
        this.lcomReductionPct = calculatePctReduction(lcomBefore, lcomAfter);
        this.forecastSummary = forecastSummary != null ? forecastSummary : "";
    }

    private static double calculatePctReduction(int before, int after) {
        if (before <= 0) return 0.0;
        if (after >= before) return 0.0;
        return Math.round(((double) (before - after) / before) * 1000.0) / 10.0;
    }

    public int getWmcBefore() {
        return wmcBefore;
    }

    public int getWmcAfter() {
        return wmcAfter;
    }

    public int getLcomBefore() {
        return lcomBefore;
    }

    public int getLcomAfter() {
        return lcomAfter;
    }

    public int getCboBefore() {
        return cboBefore;
    }

    public int getCboAfter() {
        return cboAfter;
    }

    public int getLocBefore() {
        return locBefore;
    }

    public int getLocAfter() {
        return locAfter;
    }

    public int getParamCountBefore() {
        return paramCountBefore;
    }

    public int getParamCountAfter() {
        return paramCountAfter;
    }

    public double getWmcReductionPct() {
        return wmcReductionPct;
    }

    public double getLcomReductionPct() {
        return lcomReductionPct;
    }

    public String getForecastSummary() {
        return forecastSummary;
    }

    @Override
    public String toString() {
        return String.format("Forecast [WMC: %d -> %d (-%.1f%%), LCOM: %d -> %d (-%.1f%%), CBO: %d -> %d]",
                wmcBefore, wmcAfter, wmcReductionPct,
                lcomBefore, lcomAfter, lcomReductionPct,
                cboBefore, cboAfter);
    }
}
