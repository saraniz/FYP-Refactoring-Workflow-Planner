package demo;

public class DataClumpExample {

    // Method taking 5 parameters (Data Clump / Excessive Parameter List)
    public void createCustomerProfile(String firstName, String lastName, String streetAddress, String city, String zipCode) {
        System.out.println("Customer Profile: " + firstName + " " + lastName);
        System.out.println("Address: " + streetAddress + ", " + city + " " + zipCode);
    }
}
