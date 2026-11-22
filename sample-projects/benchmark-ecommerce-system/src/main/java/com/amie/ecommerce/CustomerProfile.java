package com.amie.ecommerce;

/**
 * Domain model representing a customer account and purchase history.
 */
public class CustomerProfile {

    private String customerId;
    private String name;
    private int loyaltyPoints;
    private int totalOrdersPlaced;
    private double lifetimeSpend;
    private int yearsAsMember;

    public CustomerProfile(String customerId, String name, int loyaltyPoints, int totalOrdersPlaced, double lifetimeSpend, int yearsAsMember) {
        this.customerId = customerId;
        this.name = name;
        this.loyaltyPoints = loyaltyPoints;
        this.totalOrdersPlaced = totalOrdersPlaced;
        this.lifetimeSpend = lifetimeSpend;
        this.yearsAsMember = yearsAsMember;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public int getTotalOrdersPlaced() { return totalOrdersPlaced; }
    public double getLifetimeSpend() { return lifetimeSpend; }
    public int getYearsAsMember() { return yearsAsMember; }
}
