package com.mkr.commerce.billing.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DashedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.BorderRadius;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.mkr.commerce.billing.dto.BillConfirmResponse;
import com.mkr.commerce.billing.dto.BillLineItemResponse;
import com.mkr.commerce.billing.dto.BillPdfRequest;
import com.mkr.commerce.billing.entity.Bill;
import com.mkr.commerce.billing.repository.BillRepository;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ITextBillPdfService {

    // ── Brand colours — match frontend exactly ────────────────────────────────
    private static final DeviceRgb  BLUE        = new DeviceRgb(0x28, 0x74, 0xF0); // #2874F0
    private static final DeviceRgb  ORANGE      = new DeviceRgb(0xFF, 0x9F, 0x00); // #FF9F00
    private static final DeviceRgb  BLUE_LIGHT  = new DeviceRgb(0xEB, 0xF2, 0xFF); // #EBF2FF
    private static final DeviceRgb  BLUE_NAVY   = new DeviceRgb(0x1E, 0x40, 0xAF); // #1E40AF
    private static final DeviceRgb  GREEN_DARK  = new DeviceRgb(0x16, 0x80, 0x3E); // #16803E
    private static final DeviceRgb  RED_DARK    = new DeviceRgb(0xC0, 0x2A, 0x2A); // #C02A2A
    private static final DeviceRgb  DARK_TEXT   = new DeviceRgb(0x1E, 0x29, 0x3B); // #1e293b
    private static final DeviceRgb  MID_TEXT    = new DeviceRgb(0x47, 0x55, 0x69); // #475569
    private static final DeviceRgb  MUTED_TEXT  = new DeviceRgb(0x64, 0x74, 0x8B); // #64748b
    private static final DeviceRgb  BORDER_CLR  = new DeviceRgb(0xE2, 0xE8, 0xF0); // #e2e8f0
    private static final DeviceRgb  ROW_ALT     = new DeviceRgb(0xF8, 0xFA, 0xFF); // #f8faff

    private static final DeviceGray WHITE = new DeviceGray(1f);
    private static final DeviceGray BLACK = new DeviceGray(0f);

    // ── Type sizes ────────────────────────────────────────────────────────────
    private static final float MARGIN       = 36f;
    private static final float FS_LOGO      = 22f;
    private static final float FS_INV_NUM   = 17f;
    private static final float FS_PAY_MED   = 13f;
    private static final float FS_CUST_NAME = 12f;
    private static final float FS_HEAD      = 9f;
    private static final float FS_BODY      = 8.5f;
    private static final float FS_SMALL     = 8f;
    private static final float FS_LABEL     = 7f;
    private static final float FS_TOTAL_LBL = 8f;
    private static final float FS_TOTAL_AMT = 15f;
    private static final float FS_CHIP      = 7f;
    private static final float TOP_BAR_H    = 4f;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                         .withZone(ZoneId.of("Asia/Kolkata"));

    private final BillRepository billRepository;

    // ── Font ──────────────────────────────────────────────────────────────────

    private static final String[][] FONTS = {
        // Windows dev
        {"C:/Windows/Fonts/arial.ttf",   "C:/Windows/Fonts/arialbd.ttf"},
        // Debian/Ubuntu
        {"/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
         "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"},
        // Alpine Linux (Docker) — installed via ttf-dejavu package
        {"/usr/share/fonts/dejavu/DejaVuSans.ttf",
         "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"},
        // macOS dev
        {"/Library/Fonts/Arial.ttf",     "/Library/Fonts/Arial Bold.ttf"},
    };

    private PdfFont[] fonts() throws IOException {
        for (String[] p : FONTS) {
            try {
                return new PdfFont[]{
                    PdfFontFactory.createFont(p[1], PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED),
                    PdfFontFactory.createFont(p[0], PdfEncodings.IDENTITY_H, EmbeddingStrategy.PREFER_EMBEDDED)
                };
            } catch (Exception ignored) {}
        }
        return new PdfFont[]{
            PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD),
            PdfFontFactory.createFont(StandardFonts.HELVETICA)
        };
    }

    // ── Entry ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generate(UUID billId, BillPdfRequest shop) throws IOException {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));
        BillConfirmResponse res = BillConfirmResponse.from(bill);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer = new PdfWriter(baos);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4);
        // top margin = TOP_BAR_H + content gap; bottom = 50 for footer
        doc.setMargins(TOP_BAR_H + 20f, MARGIN, 50f, MARGIN);

        PdfFont bold = fonts()[0];
        PdfFont reg  = fonts()[1];

        // ── Content ──
        header(doc, bold, reg, shop, res);
        space(doc, 8);
        billedToRow(doc, bold, reg, res);
        space(doc, 10);
        sectionTitle(doc, reg, "ITEMS");
        space(doc, 4);
        itemsTable(doc, bold, reg, res);
        space(doc, 6);
        summary(doc, bold, reg, res);
        space(doc, 10);
        paymentSection(doc, bold, reg, res);

        // ── Fixed elements drawn directly on pages ──
        doc.flush();
        int lastPage = pdf.getNumberOfPages();
        for (int p = 1; p <= lastPage; p++) {
            drawTopGradientBar(pdf, p);
        }
        drawFooter(pdf, reg, shop, lastPage);

        doc.close();
        return baos.toByteArray();
    }

    // ── 1. Header ─────────────────────────────────────────────────────────────

    private void header(Document doc, PdfFont bold, PdfFont reg,
                        BillPdfRequest shop, BillConfirmResponse res) {

        Table t = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // Left — brand name + contact
        Cell left = noB().setPadding(0);

        // "MKR" blue + "Commerce" orange — matching website logo
        Paragraph logo = new Paragraph()
                .add(new Text("MKR").setFont(bold).setFontSize(FS_LOGO).setFontColor(BLUE))
                .add(new Text("Commerce").setFont(bold).setFontSize(FS_LOGO).setFontColor(ORANGE))
                .setMarginBottom(3);
        left.add(logo);

        if (ok(shop.shopTagline()))
            left.add(p(shop.shopTagline(), reg, FS_LABEL, MUTED_TEXT).setItalic().setMarginBottom(4));
        if (ok(shop.shopAddress()))
            left.add(p(shop.shopAddress(), reg, FS_BODY, MID_TEXT).setMarginBottom(1));

        String phones = buildPhones(shop);
        if (!phones.isEmpty())
            left.add(p(phones, reg, FS_BODY, MID_TEXT).setMarginBottom(1));
        if (ok(shop.shopEmail()))
            left.add(p(shop.shopEmail(), reg, FS_BODY, MID_TEXT).setMarginBottom(1));
        if (ok(shop.shopGstin()))
            left.add(p("GSTIN: " + shop.shopGstin(), bold, FS_BODY, DARK_TEXT));

        // Right — "INVOICE" outlined badge (blue border, blue text, white bg) — matches frontend
        Cell right = noB().setPadding(0).setTextAlignment(TextAlignment.RIGHT);

        Paragraph invLabel = new Paragraph("INVOICE")
                .setFont(bold).setFontSize(FS_LABEL + 1f).setFontColor(BLUE)
                .setBorder(new SolidBorder(BLUE, 1.5f))
                .setPaddingTop(3).setPaddingBottom(3).setPaddingLeft(10).setPaddingRight(10)
                .setBorderRadius(new BorderRadius(3))
                .setCharacterSpacing(1.8f)
                .setMarginBottom(8);
        right.add(invLabel);

        right.add(p(res.billId(), bold, FS_INV_NUM, DARK_TEXT).setMarginBottom(4));
        right.add(p(DATE_FMT.format(res.createdAt()), reg, FS_SMALL, MUTED_TEXT));

        t.addCell(left);
        t.addCell(right);
        doc.add(t);

        // Thin separator line under header
        doc.add(new Paragraph()
                .setBorderBottom(new SolidBorder(BORDER_CLR, 1f))
                .setMarginTop(10).setMarginBottom(0));
    }

    // ── 2. Billed-to + Payment ─────────────────────────────────────────────────

    private void billedToRow(Document doc, PdfFont bold, PdfFont reg,
                             BillConfirmResponse res) {

        boolean hasKhata = res.khataAmount().compareTo(BigDecimal.ZERO) > 0;

        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // BILLED TO — white bg, 3px blue top border (matches .info-box.billed)
        Cell bt = new Cell()
                .setBorder(new SolidBorder(BORDER_CLR, 0.8f))
                .setBorderTop(new SolidBorder(BLUE, 3f))
                .setBackgroundColor(WHITE)
                .setPadding(12).setMarginRight(6);

        bt.add(capLabel("BILLED TO").setMarginBottom(8));

        if (res.customer() != null) {
            var c = res.customer();
            // Customer name — large bold (matches .cust-name 13px bold)
            bt.add(p(c.name(), bold, FS_CUST_NAME, DARK_TEXT).setMarginBottom(5));
            if (ok(c.phone())) bt.add(infoLine(bold, reg, "Phone", c.phone()));
            if (ok(c.email())) bt.add(infoLine(bold, reg, "Email", c.email()));
        } else {
            bt.add(p("Walk-in Customer", reg, FS_BODY, MUTED_TEXT));
        }

        // PAYMENT — white bg, 3px top border: red if khata, green if fully paid
        DeviceRgb payBorderColor = hasKhata ? RED_DARK : GREEN_DARK;
        Cell pm = new Cell()
                .setBorder(new SolidBorder(BORDER_CLR, 0.8f))
                .setBorderTop(new SolidBorder(payBorderColor, 3f))
                .setBackgroundColor(WHITE)
                .setPadding(12).setMarginLeft(6);

        pm.add(capLabel("PAYMENT").setMarginBottom(8));

        // Payment method — large bold dark text (matches .pay-method 15px bold)
        pm.add(p(res.paymentMethod().toUpperCase(), bold, FS_PAY_MED, DARK_TEXT).setMarginBottom(6));

        // Status chips — GST, Fully Paid / Partial Payment
        Paragraph chips = new Paragraph().setMargin(0).setMarginBottom(4);
        if (res.gstEnabled()) {
            chips.add(chip("GST", BLUE, new DeviceRgb(0xBF, 0xDB, 0xFE), new DeviceRgb(0xEF, 0xF6, 0xFF), bold));
            chips.add(new Text("  "));
        }
        if (hasKhata) {
            chips.add(chip("Partial Payment", RED_DARK, new DeviceRgb(0xFE, 0xCD, 0xD3), new DeviceRgb(0xFF, 0xF1, 0xF2), bold));
        } else {
            chips.add(chip("Fully Paid", GREEN_DARK, new DeviceRgb(0xBB, 0xF7, 0xD0), new DeviceRgb(0xF0, 0xFD, 0xF4), bold));
        }
        pm.add(chips);

        if (hasKhata) {
            pm.add(infoLine(bold, reg, "Collected", fmt(res.paidNow())));
            pm.add(infoLine(bold, reg, "Khata", fmt(res.khataAmount())));
        }

        t.addCell(bt);
        t.addCell(pm);
        doc.add(t);
    }

    // ── 3. Items table ─────────────────────────────────────────────────────────

    private void itemsTable(Document doc, PdfFont bold, PdfFont reg,
                            BillConfirmResponse res) {

        boolean showDisc = res.lineItems().stream()
                .anyMatch(i -> i.discount().compareTo(BigDecimal.ZERO) > 0);
        boolean showGst  = res.gstEnabled();

        float[] cols = showDisc && showGst
                ? new float[]{4, 30, 10, 7, 12, 9, 9, 13}
                : showDisc
                ? new float[]{4, 33, 11, 8, 14, 10, 16}
                : showGst
                ? new float[]{4, 33, 11, 8, 14, 12, 16}
                : new float[]{4, 38, 13, 9, 18, 18};

        String[] heads = showDisc && showGst
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Discount", "GST %", "Amount"}
                : showDisc
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Discount", "Amount"}
                : showGst
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "GST %", "Amount"}
                : new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Amount"};

        TextAlignment[] aligns = buildAligns(heads.length);

        Table tbl = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(BORDER_CLR, 0.8f));

        // Header row — BLUE_LIGHT bg, navy text, 2px BLUE bottom border
        for (int i = 0; i < heads.length; i++) {
            Cell hc = new Cell()
                    .setBackgroundColor(BLUE_LIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(BLUE, 2f))
                    .setPaddingTop(8).setPaddingBottom(8)
                    .setPaddingLeft(7).setPaddingRight(7);
            hc.add(new Paragraph(heads[i])
                    .setFont(bold).setFontSize(FS_LABEL)
                    .setFontColor(BLUE_NAVY).setTextAlignment(aligns[i])
                    .setCharacterSpacing(0.6f).setMargin(0));
            tbl.addHeaderCell(hc);
        }

        List<BillLineItemResponse> items = res.lineItems();
        for (int i = 0; i < items.size(); i++) {
            BillLineItemResponse it = items.get(i);
            var bg = (i % 2 == 1) ? ROW_ALT : WHITE;

            java.util.List<String> vals = new java.util.ArrayList<>();
            vals.add(String.valueOf(i + 1));
            vals.add(it.productName() != null ? it.productName() : "");
            vals.add(it.productSku()  != null ? it.productSku()  : "");
            vals.add(String.valueOf(it.qty()));
            vals.add(fmt(it.unitPrice()));
            if (showDisc) vals.add(it.discount().compareTo(BigDecimal.ZERO) > 0 ? fmt(it.discount()) : "—");
            if (showGst)  vals.add(it.gstPercent().compareTo(BigDecimal.ZERO) > 0 ? it.gstPercent() + "%" : "—");
            vals.add(fmt(it.lineTotal()));

            for (int j = 0; j < vals.size(); j++) {
                boolean isAmt = (j == vals.size() - 1);
                boolean isSku = (j == 2);
                Cell dc = new Cell()
                        .setBackgroundColor(bg)
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(new DeviceRgb(0xF1, 0xF5, 0xF9), 0.8f))
                        .setPaddingTop(7).setPaddingBottom(7)
                        .setPaddingLeft(7).setPaddingRight(7);
                Paragraph vp = new Paragraph(vals.get(j))
                        .setFont(isAmt || j == 1 ? bold : reg)
                        .setFontSize(isSku ? FS_LABEL + 0.5f : FS_BODY)
                        .setFontColor(j == 0 ? MUTED_TEXT : j == 2 ? MID_TEXT : DARK_TEXT)
                        .setTextAlignment(aligns[j]).setMargin(0);
                if (isAmt) vp.setFontColor(DARK_TEXT);
                dc.add(vp);
                tbl.addCell(dc);
            }
        }
        doc.add(tbl);
    }

    // ── 4. Summary ─────────────────────────────────────────────────────────────

    private void summary(Document doc, PdfFont bold, PdfFont reg,
                         BillConfirmResponse res) {

        boolean hasDisc = res.totalDiscount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasGst  = res.gstEnabled() && res.gstAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean showSub = hasDisc || hasGst;

        // 40% width right-aligned, border all around — matches frontend .sum-box
        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setBorder(new SolidBorder(BORDER_CLR, 0.8f));

        if (showSub)
            sumRow(t, reg, bold, "Subtotal",   fmt(res.subtotal()),              MUTED_TEXT, DARK_TEXT);
        if (hasDisc)
            sumRow(t, reg, bold, "Discount",   "− " + fmt(res.totalDiscount()),  MUTED_TEXT, GREEN_DARK);
        if (hasGst)
            sumRow(t, reg, bold, "GST",        "+ " + fmt(res.gstAmount()),      MUTED_TEXT, BLUE);

        // Grand Total row — BLUE_LIGHT bg, 2px BLUE top border
        Cell gl = new Cell()
                .setBackgroundColor(BLUE_LIGHT)
                .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(BLUE, 2f))
                .setPaddingTop(11).setPaddingBottom(11).setPaddingLeft(10);
        gl.add(new Paragraph("GRAND TOTAL")
                .setFont(bold).setFontSize(FS_TOTAL_LBL).setFontColor(BLUE_NAVY)
                .setCharacterSpacing(0.6f).setMargin(0));

        Cell gr = new Cell()
                .setBackgroundColor(BLUE_LIGHT)
                .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(BLUE, 2f))
                .setPaddingTop(11).setPaddingBottom(11).setPaddingRight(10);
        gr.add(new Paragraph(fmt(res.grandTotal()))
                .setFont(bold).setFontSize(FS_TOTAL_AMT).setFontColor(ORANGE)
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0));

        t.addCell(gl);
        t.addCell(gr);
        doc.add(t);
    }

    private void sumRow(Table t, PdfFont reg, PdfFont bold,
                        String label, String value, DeviceRgb labelColor, DeviceRgb valColor) {
        SolidBorder b = new SolidBorder(new DeviceRgb(0xF1, 0xF5, 0xF9), 0.8f);

        Cell lc = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6).setPaddingLeft(10);
        lc.add(new Paragraph(label).setFont(reg).setFontSize(FS_BODY).setFontColor(labelColor).setMargin(0));

        Cell vc = new Cell().setBorder(Border.NO_BORDER).setBorderBottom(b).setPadding(6).setPaddingRight(10);
        vc.add(new Paragraph(value).setFont(bold).setFontSize(FS_BODY).setFontColor(valColor)
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0));

        t.addCell(lc);
        t.addCell(vc);
    }

    // ── 5. Payment section ─────────────────────────────────────────────────────

    private void paymentSection(Document doc, PdfFont bold, PdfFont reg,
                                BillConfirmResponse res) {

        boolean hasKhata = res.khataAmount().compareTo(BigDecimal.ZERO) > 0;

        Table box = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(new SolidBorder(BORDER_CLR, 0.8f));

        // Header band — light bg, blue spaced uppercase text
        Cell hdr = new Cell(1, 2)
                .setBackgroundColor(new DeviceRgb(0xF8, 0xFA, 0xFC))
                .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(BORDER_CLR, 0.8f))
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(14);
        hdr.add(new Paragraph("PAYMENT SUMMARY")
                .setFont(bold).setFontSize(FS_LABEL)
                .setFontColor(BLUE).setCharacterSpacing(1.2f).setMargin(0));
        box.addCell(hdr);

        payRow(box, reg, bold, "Payment Method",   res.paymentMethod().toUpperCase(), BLUE);
        payRow(box, reg, bold, "Amount Collected",  fmt(res.paidNow()),               GREEN_DARK);

        if (hasKhata) {
            payRow(box, reg, bold, "Deferred to Khata", fmt(res.khataAmount()), RED_DARK);

            Cell note = new Cell(1, 2)
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(new DeviceRgb(0xFE, 0xCA, 0xCA), 0.8f))
                    .setBackgroundColor(new DeviceRgb(0xFF, 0xF8, 0xF8))
                    .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(14).setPaddingRight(14);
            String custName = (res.customer() != null) ? res.customer().name() : "customer";
            note.add(new Paragraph(fmt(res.khataAmount()) + " has been recorded in "
                    + custName + "'s Khata. Check the Khata tab for the current outstanding balance.")
                    .setFont(reg).setFontSize(FS_SMALL).setFontColor(MUTED_TEXT)
                    .setItalic().setMargin(0));
            box.addCell(note);
        }

        doc.add(box);
    }

    private void payRow(Table t, PdfFont reg, PdfFont bold,
                        String label, String value, DeviceRgb valColor) {
        SolidBorder border = new SolidBorder(new DeviceRgb(0xF8, 0xFA, 0xFC), 0.8f);

        Cell lc = new Cell()
                .setBorder(Border.NO_BORDER).setBorderTop(border)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(14);
        lc.add(new Paragraph(label).setFont(reg).setFontSize(FS_BODY).setFontColor(MUTED_TEXT).setMargin(0));

        Cell vc = new Cell()
                .setBorder(Border.NO_BORDER).setBorderTop(border)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingRight(14);
        vc.add(new Paragraph(value).setFont(bold).setFontSize(FS_BODY).setFontColor(valColor)
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0));

        t.addCell(lc);
        t.addCell(vc);
    }

    // ── 6. Top gradient bar — drawn on canvas on every page ───────────────────
    // Approximates CSS linear-gradient(to right, #2874F0, #FF9F00) using 12 steps

    private void drawTopGradientBar(PdfDocument pdf, int pageNum) {
        PdfPage page = pdf.getPage(pageNum);
        float w = page.getPageSize().getWidth();
        float h = page.getPageSize().getHeight();

        // Blue → Orange: #2874F0 → #FF9F00
        float[] fromR = {0x28/255f, 0x74/255f, 0xF0/255f};
        float[] toR   = {0xFF/255f, 0x9F/255f, 0x00/255f};

        int steps = 12;
        float segW = w / steps;

        PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdf);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (steps - 1);
            float r = fromR[0] + t * (toR[0] - fromR[0]);
            float g = fromR[1] + t * (toR[1] - fromR[1]);
            float b = fromR[2] + t * (toR[2] - fromR[2]);
            canvas.setFillColor(new DeviceRgb(r, g, b))
                  .rectangle(i * segW, h - TOP_BAR_H, segW + 0.5f, TOP_BAR_H)
                  .fill();
        }
        canvas.release();
    }

    // ── 7. Footer — dashed rule + centered text ───────────────────────────────

    private void drawFooter(PdfDocument pdf, PdfFont reg, BillPdfRequest shop, int lastPage) throws IOException {
        PdfPage page = pdf.getPage(lastPage);
        float w = page.getPageSize().getWidth();
        float y = 20f;

        PdfCanvas canvas = new PdfCanvas(page);

        // Dashed rule
        canvas.setStrokeColor(new DeviceRgb(0xE2, 0xE8, 0xF0))
              .setLineWidth(0.8f)
              .setLineDash(3f, 3f)
              .moveTo(MARGIN, y + 22).lineTo(w - MARGIN, y + 22).stroke()
              .setLineDash(1f); // reset dash

        String name = ok(shop.shopName()) ? shop.shopName() : "our store";
        Rectangle rect = new Rectangle(MARGIN, y, w - 2 * MARGIN, 20f);
        try (Canvas c2 = new Canvas(canvas, rect)) {
            c2.add(new Paragraph(
                    "Thank you for shopping at " + name + "  ·  Computer generated invoice — no signature required.")
                    .setFont(reg).setFontSize(FS_SMALL).setFontColor(MUTED_TEXT)
                    .setTextAlignment(TextAlignment.CENTER).setMargin(0));
        }
        canvas.release();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Section title above items table — "ITEMS" small uppercase gray */
    private void sectionTitle(Document doc, PdfFont reg, String text) {
        doc.add(new Paragraph(text)
                .setFont(reg).setFontSize(FS_LABEL - 0.5f).setFontColor(MUTED_TEXT)
                .setCharacterSpacing(1.4f)
                .setBorderBottom(new SolidBorder(BORDER_CLR, 1.5f))
                .setPaddingBottom(5).setMarginBottom(0));
    }

    /** Small uppercase blue label — "BILLED TO", "PAYMENT" */
    private Paragraph capLabel(String text) {
        return new Paragraph(text)
                .setFontSize(FS_LABEL)
                .setFontColor(BLUE)
                .setCharacterSpacing(1.2f)
                .setBold()
                .setBorderBottom(new SolidBorder(BORDER_CLR, 0.8f))
                .setPaddingBottom(5)
                .setMargin(0);
    }

    /** "Key  : Value" info line inside billed-to */
    private Paragraph infoLine(PdfFont bold, PdfFont reg, String label, String value) {
        if (value == null || value.isBlank()) return new Paragraph().setMargin(0);
        Paragraph p = new Paragraph().setMarginBottom(3);
        p.add(new Text(label).setFont(bold).setFontSize(FS_SMALL - 0.5f).setFontColor(MUTED_TEXT));
        p.add(new Text("  " + value).setFont(reg).setFontSize(FS_BODY).setFontColor(DARK_TEXT));
        return p;
    }

    /** Inline pill chip — "GST", "Fully Paid", "Partial Payment" */
    private Text chip(String label, DeviceRgb textColor, DeviceRgb borderColor,
                      DeviceRgb bgColor, PdfFont bold) {
        return new Text(" " + label + " ")
                .setFont(bold)
                .setFontSize(FS_CHIP)
                .setFontColor(textColor)
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(borderColor, 0.8f))
                .setBorderRadius(new BorderRadius(10));
    }

    private Paragraph p(String text, PdfFont font, float size, com.itextpdf.kernel.colors.Color color) {
        return new Paragraph(text).setFont(font).setFontSize(size).setFontColor(color).setMargin(0);
    }

    private Cell noB() { return new Cell().setBorder(Border.NO_BORDER); }

    private void space(Document doc, float h) {
        doc.add(new Paragraph().setMarginTop(h).setMarginBottom(0));
    }

    private boolean ok(String s) { return s != null && !s.isBlank(); }

    private String buildPhones(BillPdfRequest s) {
        StringBuilder sb = new StringBuilder();
        if (ok(s.shopPhone()))  sb.append("Ph: ").append(s.shopPhone());
        if (ok(s.shopPhone2())) { if (!sb.isEmpty()) sb.append("  /  "); sb.append(s.shopPhone2()); }
        return sb.toString();
    }

    private String fmt(BigDecimal a) {
        if (a == null) return "0.00";
        return "₹" + String.format("%,.2f", a);
    }

    private TextAlignment[] buildAligns(int count) {
        TextAlignment[] a = new TextAlignment[count];
        a[0] = TextAlignment.CENTER;
        a[1] = TextAlignment.LEFT;
        a[2] = TextAlignment.LEFT;
        a[3] = TextAlignment.CENTER;
        for (int i = 4; i < count; i++) a[i] = TextAlignment.RIGHT;
        return a;
    }
}
