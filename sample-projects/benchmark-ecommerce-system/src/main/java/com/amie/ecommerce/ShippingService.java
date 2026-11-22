package com.amie.ecommerce;

/**
 * ShippingService demonstrates the DATA_CLUMPS code smell.
 * The address parameters (street, city, state, zipCode, country) appear together
 * across multiple method signatures and should be refactored using Introduce Parameter Object.
 */
public class ShippingService {

    public double calculateStandardShipping(String street, String city, String state, String zipCode, String country, double packageWeight) {
        if ("USA".equalsIgnoreCase(country)) {
            return 5.0 + packageWeight * 1.5;
        }
        return 20.0 + packageWeight * 4.0;
    }

    public double calculateExpressShipping(String street, String city, String state, String zipCode, String country, double packageWeight) {
        if ("USA".equalsIgnoreCase(country)) {
            return 15.0 + packageWeight * 3.0;
        }
        return 45.0 + packageWeight * 7.5;
    }

    public String generateDeliveryLabel(String street, String city, String state, String zipCode, String country, String recipientName) {
        return recipientName + "\n" + street + "\n" + city + ", " + state + " " + zipCode + "\n" + country;
    }
}
