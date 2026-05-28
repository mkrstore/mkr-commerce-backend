package com.mkr.commerce.billing.dto;

import com.mkr.commerce.billing.entity.Bill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BillConfirmResponse(
        UUID                    id,
        String                  billId,          // MKR-BILL-0001
        String                  paymentMethod,
        BigDecimal              subtotal,
        BigDecimal              totalDiscount,
        BigDecimal              gstAmount,
        BigDecimal              grandTotal,
        BigDecimal              paidNow,
        BigDecimal              khataAmount,
        boolean                 gstEnabled,
        BillCustomerDto         customer,
        List<BillLineItemResponse> lineItems,
        Instant                 createdAt
) {
    public static BillConfirmResponse from(Bill b) {
        return new BillConfirmResponse(
                b.getId(),
                b.getBillId(),
                b.getPaymentMethod(),
                b.getSubtotal(),
                b.getTotalDiscount(),
                b.getGstAmount(),
                b.getGrandTotal(),
                b.getPaidNow(),
                b.getKhataAmount(),
                b.isGstEnabled(),
                b.getCustomer() != null ? BillCustomerDto.from(b.getCustomer()) : null,
                b.getLineItems().stream().map(BillLineItemResponse::from).toList(),
                b.getCreatedAt()
        );
    }
}
