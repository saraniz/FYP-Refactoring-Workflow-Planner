package com.amie.ecommerce;

/**
 * CustomerReportService exhibits the FEATURE_ENVY smell.
 * Its methods primarily extract fields from CustomerProfile rather than operating on its own state.
 */
public class CustomerReportService {

    public String computeVipTierClassification(CustomerProfile customer) {
        // Envious logic accessing customer internals repeatedly:
        double spend = customer.getLifetimeSpend();
        int points = customer.getLoyaltyPoints();
        int orders = customer.getTotalOrdersPlaced();
        int tenure = customer.getYearsAsMember();

        if (spend > 10000.0 && points > 5000 && tenure >= 3) {
            return "DIAMOND_PREMIER";
        } else if (spend > 5000.0 || points > 2000) {
            return "GOLD_TIER";
        } else if (orders >= 5) {
            return "SILVER_TIER";
        }
        return "BRONZE_STANDARD";
    }

    public double calculateLoyaltyBonus(CustomerProfile customer, double purchaseAmount) {
        if (customer.getLifetimeSpend() > 2000.0 && customer.getYearsAsMember() > 2) {
            return purchaseAmount * 0.05 + customer.getLoyaltyPoints() * 0.01;
        }
        return purchaseAmount * 0.02;
    }
}
