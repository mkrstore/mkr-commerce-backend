package com.mkr.commerce.billing.controller;

import com.mkr.commerce.billing.dto.BillEmailRequest;
import com.mkr.commerce.billing.dto.BillPdfRequest;
import com.mkr.commerce.billing.service.BillEmailService;
import com.mkr.commerce.billing.service.ITextBillPdfService;
import com.mkr.commerce.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GET  /api/billing/{id}/pdf          — download invoice as PDF
 * POST /api/billing/{id}/send-email   — email invoice to customer via Resend
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillReportController {

    private final ITextBillPdfService pdfService;
    private final BillEmailService    emailService;

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','INVENTORY')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @ModelAttribute BillPdfRequest request
    ) throws Exception {

        byte[] pdf = pdfService.generate(id, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("bill-" + id + ".pdf")
                        .build());
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<String>> sendEmail(
            @PathVariable UUID id,
            @RequestBody BillEmailRequest request
    ) throws Exception {

        emailService.sendBillEmail(id, request);

        return ResponseEntity.ok(ApiResponse.ok("Email sent", "Invoice emailed successfully."));
    }
}
