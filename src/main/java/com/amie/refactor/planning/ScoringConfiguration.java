package com.amie.refactor.planning;

/**
 * Configurable thresholds and saturation bounds used for normalizing software metrics
 * and dependency indicators into [0.0, 1.0] interval.
 *
 * <p>Exposes sensible defaults based on empirical literature (Chidamber & Kemerer,
 * Lanza & Marinescu, Fontana et al.) while allowing researchers to fine-tune parameters.</p>
 */
public class ScoringConfiguration {

    // Thresholds above which metrics saturate at 1.0 (highest concern)
    private double wmcSaturationThreshold = 40.0;
    private double cboSaturationThreshold = 12.0;
    private double lcomSaturationThreshold = 35.0;
    private double methodLocSaturationThreshold = 50.0;
    private double cyclomaticComplexitySaturationThreshold = 15.0;
    private double dependencySaturationThreshold = 10.0;

    public ScoringConfiguration() {}

    public double getWmcSaturationThreshold() {
        return wmcSaturationThreshold;
    }

    public void setWmcSaturationThreshold(double wmcSaturationThreshold) {
        this.wmcSaturationThreshold = wmcSaturationThreshold;
    }

    public double getCboSaturationThreshold() {
        return cboSaturationThreshold;
    }

    public void setCboSaturationThreshold(double cboSaturationThreshold) {
        this.cboSaturationThreshold = cboSaturationThreshold;
    }

    public double getLcomSaturationThreshold() {
        return lcomSaturationThreshold;
    }

    public void setLcomSaturationThreshold(double lcomSaturationThreshold) {
        this.lcomSaturationThreshold = lcomSaturationThreshold;
    }

    public double getMethodLocSaturationThreshold() {
        return methodLocSaturationThreshold;
    }

    public void setMethodLocSaturationThreshold(double methodLocSaturationThreshold) {
        this.methodLocSaturationThreshold = methodLocSaturationThreshold;
    }

    public double getCyclomaticComplexitySaturationThreshold() {
        return cyclomaticComplexitySaturationThreshold;
    }

    public void setCyclomaticComplexitySaturationThreshold(double cyclomaticComplexitySaturationThreshold) {
        this.cyclomaticComplexitySaturationThreshold = cyclomaticComplexitySaturationThreshold;
    }

    public double getDependencySaturationThreshold() {
        return dependencySaturationThreshold;
    }

    public void setDependencySaturationThreshold(double dependencySaturationThreshold) {
        this.dependencySaturationThreshold = dependencySaturationThreshold;
    }
}
