package com.mkr.commerce.billing.service;

import com.mkr.commerce.billing.dto.BillConfirmResponse;
import com.mkr.commerce.billing.dto.BillPdfRequest;
import com.mkr.commerce.billing.entity.Bill;
import com.mkr.commerce.billing.repository.BillRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private static final String WA_BASE = "https://graph.facebook.com/v21.0";

    @Value("${whatsapp.api-token:}")
    private String apiToken;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    private final BillRepository      billRepository;
    private final ITextBillPdfService pdfService;
    private final RestTemplate        restTemplate;

    public boolean isConfigured() {
        return StringUtils.hasText(apiToken) && StringUtils.hasText(phoneNumberId);
    }

    @Transactional(readOnly = true)
    public void sendBillToWhatsApp(UUID billId, BillPdfRequest shop) throws Exception {
        if (!isConfigured()) {
            throw new BadRequestException(
                "WhatsApp Business not configured. Set whatsapp.api-token and whatsapp.phone-number-id.");
        }

        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        BillConfirmResponse res = BillConfirmResponse.from(bill);

        if (res.customer() == null || !StringUtils.hasText(res.customer().phone())) {
            throw new BadRequestException("Customer phone number not available on this bill.");
        }

        byte[] pdfBytes = pdfService.generate(billId, shop);

        String toPhone  = normalizePhone(res.customer().phone());
        String mediaId  = uploadMedia(pdfBytes, res.billId() + ".pdf");
        String caption  = buildCaption(res, shop);

        sendDocumentMessage(toPhone, mediaId, res.billId() + ".pdf", caption);
        log.info("WhatsApp invoice sent to {} for {}", toPhone, res.billId());
    }

    // ── 1. Upload PDF bytes to WhatsApp media endpoint ────────────────────────

    @SuppressWarnings("unchecked")
    private String uploadMedia(byte[] pdfBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiToken);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("type", "application/pdf");
        body.add("file", new ByteArrayResource(pdfBytes) {
            @Override public String getFilename() { return filename; }
        });

        ResponseEntity<Map> resp = restTemplate.postForEntity(
            WA_BASE + "/" + phoneNumberId + "/media",
            new HttpEntity<>(body, headers),
            Map.class
        );

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("WhatsApp media upload failed: " + resp.getStatusCode());
        }
        String mediaId = (String) resp.getBody().get("id");
        if (!StringUtils.hasText(mediaId)) {
            throw new RuntimeException("WhatsApp media upload returned no media ID");
        }
        return mediaId;
    }

    // ── 2. Send document message ──────────────────────────────────────────────

    private void sendDocumentMessage(String to, String mediaId, String filename, String caption) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to",   to,
            "type", "document",
            "document", Map.of(
                "id",       mediaId,
                "filename", filename,
                "caption",  caption
            )
        );

        ResponseEntity<String> resp = restTemplate.postForEntity(
            WA_BASE + "/" + phoneNumberId + "/messages",
            new HttpEntity<>(payload, headers),
            String.class
        );

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("WhatsApp send failed: " + resp.getStatusCode() + " " + resp.getBody());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("91") && digits.length() > 10) return digits;
        return "91" + digits;
    }

    private String buildCaption(BillConfirmResponse res, BillPdfRequest shop) {
        String custName = res.customer() != null ? res.customer().name() : "Customer";
        String paid     = res.khataAmount().compareTo(BigDecimal.ZERO) > 0
            ? "₹" + res.paidNow() + " paid, ₹" + res.khataAmount() + " on khata"
            : "₹" + res.grandTotal() + " paid";

        return "🧾 Invoice " + res.billId() + " from " + shop.shopName() + "\n"
             + "Dear " + custName + ", thank you for your purchase!\n"
             + "Amount: " + paid + "\n"
             + "Please find your invoice attached.";
    }
}
