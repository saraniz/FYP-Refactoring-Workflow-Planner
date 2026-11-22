package com.amie.ecommerce;

/**
 * PaymentProcessor exhibits the GOD_CLASS code smell.
 * It combines payment gateway handling, fraud detection, ledger recording,
 * crypto conversions, refund auditing, and receipt generation in one monolithic class.
 */
public class PaymentProcessor {

    private String apiKey;
    private String merchantId;
    private double dailyTransactionTotal;
    private int fraudAlertCount;
    private String auditLogDirectory;
    private String defaultCurrency;

    public boolean processCreditCard(String cardNum, String expDate, String cvv, double amount) {
        if (cardNum != null && amount > 0) {
            dailyTransactionTotal += amount;
            return true;
        }
        return false;
    }

    public boolean processDebitCard(String cardNum, String pin, double amount) {
        return amount > 0 && pin != null;
    }

    public boolean processPayPal(String paypalEmail, double amount) {
        return paypalEmail != null && paypalEmail.contains("@");
    }

    public boolean processCryptoTransfer(String walletAddress, double btcAmount) {
        return walletAddress != null && btcAmount > 0;
    }

    public double calculateDynamicFee(double amount, String paymentType) {
        if ("CRYPTO".equals(paymentType)) return amount * 0.015;
        if ("CREDIT".equals(paymentType)) return amount * 0.029;
        return amount * 0.01;
    }

    public void flagSuspiciousActivity(String transactionId, String reason) {
        fraudAlertCount++;
        System.out.println("ALERT: Fraud flagged for " + transactionId + ": " + reason);
    }

    public boolean issueRefund(String transactionId, double refundAmount) {
        return refundAmount > 0 && transactionId != null;
    }

    public void archiveTransactionLedger(String transactionId, String date) {
        System.out.println("Archiving transaction " + transactionId + " on " + date);
    }

    public void printThermalReceipt(String transactionId, double amount, String customerName) {
        System.out.println("Receipt: Trans#" + transactionId + " Cust: " + customerName + " Amt: " + amount);
    }

    public String generatePdfInvoice(String transactionId, double amount) {
        return "INVOICE-PDF-" + transactionId + "-" + amount;
    }

    public void syncExternalLedger(String remoteEndpoint) {
        System.out.println("Syncing ledger to " + remoteEndpoint);
    }

    public int getFraudAlertCount() {
        return fraudAlertCount;
    }
}
