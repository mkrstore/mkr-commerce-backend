package com.mkr.commerce.khata.enums;

public enum KhataEntryType {
    AUTO_ORDER,     // created automatically when an order is placed unpaid
    MANUAL_DEBIT,   // staff manually gave credit (customer owes)
    MANUAL_CREDIT   // staff manually recorded a payment received
}
