package demo;

public class GodClassExample {

    // Many different responsibilities
    private String studentName;
    private String studentAddress;
    private String courseName;
    private double courseFee;
    private String invoiceNumber;
    private String paymentMethod;

    // Student management
    public void registerStudent() {
        System.out.println("Registering student");
    }

    public void updateStudentDetails() {
        System.out.println("Updating student details");
    }

    public void deleteStudent() {
        System.out.println("Deleting student");
    }

    // Course management
    public void createCourse() {
        System.out.println("Creating course");
    }

    public void updateCourse() {
        System.out.println("Updating course");
    }

    public void deleteCourse() {
        System.out.println("Deleting course");
    }

    // Payment management
    public void processPayment() {
        System.out.println("Processing payment");
    }

    public void refundPayment() {
        System.out.println("Refunding payment");
    }

    // Invoice management
    public void generateInvoice() {
        System.out.println("Generating invoice");
    }

    public void sendInvoice() {
        System.out.println("Sending invoice");
    }

    // Reporting
    public void generateStudentReport() {
        System.out.println("Generating student report");
    }

    public void generateCourseReport() {
        System.out.println("Generating course report");
    }

    public void generatePaymentReport() {
        System.out.println("Generating payment report");
    }
}