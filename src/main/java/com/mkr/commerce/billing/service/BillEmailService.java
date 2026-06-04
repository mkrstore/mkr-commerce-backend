package com.mkr.commerce.billing.service;

import com.mkr.commerce.billing.dto.BillConfirmResponse;
import com.mkr.commerce.billing.dto.BillEmailRequest;
import com.mkr.commerce.billing.dto.BillPdfRequest;
import com.mkr.commerce.billing.entity.Bill;
import com.mkr.commerce.billing.repository.BillRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillEmailService {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                         .withZone(ZoneId.of("Asia/Kolkata"));

    private final BillRepository      billRepository;
    private final ITextBillPdfService pdfService;
    private final JavaMailSender      mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Transactional(readOnly = true)
    public void sendBillEmail(UUID billId, BillEmailRequest req) throws Exception {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        BillConfirmResponse res = BillConfirmResponse.from(bill);
        String toEmail = resolveRecipient(req, res);

        BillPdfRequest shop = new BillPdfRequest(
                req.shopName(), req.shopTagline(), req.shopAddress(),
                req.shopPhone(), req.shopPhone2(), req.shopEmail(), req.shopGstin());

        byte[] pdfBytes = pdfService.generate(billId, shop);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, shop.shopName() != null ? shop.shopName() : "MKR Store");
        helper.setTo(toEmail);
        helper.setSubject("Invoice " + res.billId() + " from " + shop.shopName());
        helper.setText(buildHtmlBody(res, shop), true);
        helper.addAttachment(res.billId() + ".pdf",
                new ByteArrayResource(pdfBytes), "application/pdf");

        mailSender.send(message);
        log.info("Bill email sent to {} for {}", toEmail, res.billId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveRecipient(BillEmailRequest req, BillConfirmResponse res) {
        if (req.toEmail() != null && !req.toEmail().isBlank()) return req.toEmail().trim();
        if (res.customer() != null && res.customer().email() != null
                && !res.customer().email().isBlank()) return res.customer().email().trim();
        throw new BadRequestException(
            "No recipient email — provide toEmail or ensure the customer has an email on file.");
    }

    private String buildHtmlBody(BillConfirmResponse res, BillPdfRequest shop) {
        String customerName = res.customer() != null ? res.customer().name() : "Valued Customer";
        String date         = DATE_FMT.format(res.createdAt());
        String blue         = "#2874F0";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>")
          .append("<body style='font-family:Helvetica,Arial,sans-serif;background:#f4f4f4;margin:0;padding:0;'>")
          .append("<table width='100%' cellpadding='0' cellspacing='0'>")
          .append("<tr><td align='center' style='padding:30px 10px;'>")
          .append("<table width='600' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08);'>");

        // Header
        sb.append("<tr><td style='background:").append(blue).append(";padding:24px 30px;'>")
          .append("<h1 style='margin:0;color:#fff;font-size:22px;'>").append(esc(shop.shopName())).append("</h1>");
        if (ok(shop.shopTagline()))
            sb.append("<p style='margin:4px 0 0;color:#c8dcff;font-size:13px;'>").append(esc(shop.shopTagline())).append("</p>");
        sb.append("</td></tr>");

        // Body
        sb.append("<tr><td style='padding:30px;'>")
          .append("<p style='margin:0 0 8px;font-size:15px;color:#212121;'>Dear <strong>").append(esc(customerName)).append("</strong>,</p>")
          .append("<p style='margin:0 0 20px;font-size:14px;color:#616161;'>Your invoice <strong>")
          .append(esc(res.billId())).append("</strong> is attached. Here's a summary:</p>");

        // Summary table
        sb.append("<table width='100%' cellpadding='8' cellspacing='0' style='border-collapse:collapse;font-size:13px;margin-bottom:24px;'>");
        summaryRow(sb, "Invoice No.", res.billId(), blue, true);
        summaryRow(sb, "Date",        date,           null, false);
        summaryRow(sb, "Payment",     res.paymentMethod(), null, false);
        summaryRow(sb, "Subtotal",    fmt(res.subtotal()), null, false);
        if (res.totalDiscount().compareTo(BigDecimal.ZERO) > 0)
            summaryRow(sb, "Discount", "− " + fmt(res.totalDiscount()), null, false);
        if (res.gstEnabled())
            summaryRow(sb, "GST",       "+ " + fmt(res.gstAmount()), null, false);
        summaryRow(sb, "Grand Total", fmt(res.grandTotal()), blue, true);
        if (res.khataAmount().compareTo(BigDecimal.ZERO) > 0) {
            summaryRow(sb, "Paid Now",  fmt(res.paidNow()), null, false);
            summaryRow(sb, "Khata Due", fmt(res.khataAmount()), "#D32F2F", true);
        }
        sb.append("</table>");

        // Items
        sb.append("<h3 style='font-size:13px;color:#424242;margin:0 0 8px;'>Items</h3>")
          .append("<table width='100%' cellpadding='7' cellspacing='0' style='border-collapse:collapse;font-size:12px;'>")
          .append("<tr style='background:").append(blue).append(";color:#fff;'>")
          .append("<th align='left'>Item</th><th align='center'>Qty</th>")
          .append("<th align='right'>Unit Price</th><th align='right'>Total</th></tr>");
        var items = res.lineItems();
        for (int i = 0; i < items.size(); i++) {
            var  it = items.get(i);
            String bg = i % 2 == 1 ? "#f0f5ff" : "#fff";
            sb.append("<tr style='background:").append(bg).append(";'>")
              .append("<td>").append(esc(it.productName())).append("</td>")
              .append("<td align='center'>").append(it.qty()).append("</td>")
              .append("<td align='right'>").append(fmt(it.unitPrice())).append("</td>")
              .append("<td align='right'><strong>").append(fmt(it.lineTotal())).append("</strong></td></tr>");
        }
        sb.append("</table></td></tr>");

        // Footer
        sb.append("<tr><td style='background:#f8f8f8;padding:16px 30px;border-top:1px solid #eee;text-align:center;font-size:11px;color:#9e9e9e;'>")
          .append("Thank you for shopping at ").append(esc(shop.shopName())).append("!<br>")
          .append("This is a computer-generated invoice.");
        if (ok(shop.shopPhone()))
            sb.append("<br>Support: ").append(esc(shop.shopPhone()));
        sb.append("</td></tr></table></td></tr></table></body></html>");
        return sb.toString();
    }

    private void summaryRow(StringBuilder sb, String label, String value,
                            String color, boolean bold) {
        String valStyle = bold
            ? "font-weight:bold;color:" + (color != null ? color : "#212121") + ";"
            : "color:#424242;";
        sb.append("<tr style='border-bottom:1px solid #f0f0f0;'>")
          .append("<td style='color:#616161;'>").append(esc(label)).append("</td>")
          .append("<td align='right' style='").append(valStyle).append("'>").append(esc(value)).append("</td></tr>");
    }

    private String fmt(BigDecimal a) {
        return a == null ? "₹0.00" : "₹" + String.format("%,.2f", a);
    }

    private boolean ok(String s) { return s != null && !s.isBlank(); }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
