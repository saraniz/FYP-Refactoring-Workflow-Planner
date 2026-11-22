package com.amie.ecommerce;

/**
 * OrderService manages customer purchase flows.
 * The method `executeOrderCheckoutWorkflow` violates NCSS limits (LONG_METHOD smell)
 * by mixing inventory validation, pricing, taxation, payment, notifications, and telemetry.
 */
public class OrderService {

    private PaymentProcessor paymentProcessor;
    private InventoryService inventoryService;

    public OrderService() {
        this.paymentProcessor = new PaymentProcessor();
        this.inventoryService = new InventoryService();
    }

    public boolean executeOrderCheckoutWorkflow(String orderId, String customerId, String sku, int quantity,
                                                String cardNum, String expDate, String cvv, double unitPrice) {
        // Step 1: Pre-validation
        if (orderId == null || orderId.trim().isEmpty()) {
            System.err.println("Validation failed: order ID is required.");
            return false;
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            System.err.println("Validation failed: customer ID is required.");
            return false;
        }
        if (quantity <= 0 || quantity > 100) {
            System.err.println("Validation failed: invalid order quantity.");
            return false;
        }

        // Step 2: Stock availability verification
        boolean inStock = inventoryService.isAvailable(sku, quantity);
        if (!inStock) {
            System.err.println("Stock shortage for SKU: " + sku);
            return false;
        }

        // Step 3: Complex price and tax calculation
        double subtotal = unitPrice * quantity;
        double taxRate = 0.08;
        if (subtotal > 500.0) {
            taxRate = 0.06;
        }
        double taxAmount = subtotal * taxRate;
        double shippingCost = (subtotal > 100.0) ? 0.0 : 15.0;
        double grandTotal = subtotal + taxAmount + shippingCost;

        // Step 4: Payment invocation
        boolean paymentSuccess = paymentProcessor.processCreditCard(cardNum, expDate, cvv, grandTotal);
        if (!paymentSuccess) {
            paymentProcessor.flagSuspiciousActivity(orderId, "Payment declined during checkout");
            return false;
        }

        // Step 5: Inventory decrement
        inventoryService.deductStock(sku, quantity);

        // Step 6: Post-purchase invoicing and audit logging
        paymentProcessor.archiveTransactionLedger(orderId, "2026-11-10");
        String invoice = paymentProcessor.generatePdfInvoice(orderId, grandTotal);
        System.out.println("Generated invoice: " + invoice);

        // Step 7: Telemetry tracking
        System.out.println("Order completed successfully: " + orderId + ", Total: $" + grandTotal);
        return true;
    }
}
