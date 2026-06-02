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
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
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

    // ── Brand colours (match website) ─────────────────────────────────────────
    private static final DeviceRgb  BLUE        = new DeviceRgb(0x28, 0x74, 0xF0); // #2874F0
    private static final DeviceRgb  ORANGE      = new DeviceRgb(0xFF, 0x9F, 0x00); // #FF9F00
    private static final DeviceRgb  BLUE_LIGHT  = new DeviceRgb(0xEB, 0xF2, 0xFF); // #EBF2FF table/total bg
    private static final DeviceRgb  BLUE_NAVY   = new DeviceRgb(0x1E, 0x40, 0xAF); // #1E40AF dark blue label
    private static final DeviceRgb  GREEN_DARK  = new DeviceRgb(0x16, 0x80, 0x3E);
    private static final DeviceRgb  RED_DARK    = new DeviceRgb(0xC0, 0x2A, 0x2A);

    // Grays — all dark enough to print clearly in B&W
    private static final DeviceGray WHITE       = new DeviceGray(1f);
    private static final DeviceGray BLACK       = new DeviceGray(0f);
    private static final DeviceGray G10         = new DeviceGray(0.10f); // near-black text
    private static final DeviceGray G35         = new DeviceGray(0.35f); // secondary text
    private static final DeviceGray G55         = new DeviceGray(0.55f); // muted labels
    private static final DeviceGray G85         = new DeviceGray(0.85f); // light borders
    private static final DeviceGray G94         = new DeviceGray(0.94f); // subtle row fill

    // Sizes
    private static final float MARGIN    = 36f;
    private static final float FS_LOGO   = 21f;
    private static final float FS_HEAD   = 10.5f;
    private static final float FS_BODY   = 9f;
    private static final float FS_SMALL  = 7.5f;
    private static final float FS_LABEL  = 7f;
    private static final float FS_TOTAL  = 11f;

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
                         .withZone(ZoneId.of("Asia/Kolkata"));

    private final BillRepository billRepository;

    // ── Font ──────────────────────────────────────────────────────────────────

    private static final String[][] FONTS = {
        {"C:/Windows/Fonts/arial.ttf",   "C:/Windows/Fonts/arialbd.ttf"},
        {"/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
         "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"},
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
        doc.setMargins(MARGIN, MARGIN, 50f, MARGIN); // bottom=50 for fixed footer

        PdfFont bold = fonts()[0];
        PdfFont reg  = fonts()[1];

        // ── Content ──
        header(doc, bold, reg, shop, res);
        space(doc, 8);
        billedToRow(doc, bold, reg, res);
        space(doc, 10);
        itemsTable(doc, bold, reg, res);
        space(doc, 6);
        summary(doc, bold, reg, res);
        space(doc, 10);
        paymentSection(doc, bold, reg, res);

        // ── Fixed footer on last page ──
        doc.flush();
        int lastPage = pdf.getNumberOfPages();
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

        // "MKR" blue + "Commerce" orange (matching website logo)
        Paragraph logo = new Paragraph()
                .add(new Text("MKR").setFont(bold).setFontSize(FS_LOGO).setFontColor(BLUE))
                .add(new Text("Commerce").setFont(bold).setFontSize(FS_LOGO).setFontColor(ORANGE))
                .setMarginBottom(3);
        left.add(logo);

        if (ok(shop.shopTagline()))
            left.add(p(shop.shopTagline(), reg, FS_SMALL, G55).setMarginBottom(2));
        if (ok(shop.shopAddress()))
            left.add(p(shop.shopAddress(), reg, FS_BODY, G10).setMarginBottom(1));

        String phones = buildPhones(shop);
        if (!phones.isEmpty())
            left.add(p(phones, reg, FS_BODY, G10).setMarginBottom(1));
        if (ok(shop.shopEmail()))
            left.add(p(shop.shopEmail(), reg, FS_BODY, G10).setMarginBottom(1));
        if (ok(shop.shopGstin()))
            left.add(p("GSTIN: " + shop.shopGstin(), bold, FS_BODY, G10));

        // Right — "INVOICE" box with blue background
        Cell right = noB().setPadding(0).setTextAlignment(TextAlignment.RIGHT);

        // Blue badge "INVOICE"
        Paragraph invLabel = new Paragraph("  INVOICE  ")
                .setFont(bold).setFontSize(FS_HEAD).setFontColor(WHITE)
                .setBackgroundColor(BLUE)
                .setPaddingTop(3).setPaddingBottom(3).setPaddingLeft(8).setPaddingRight(8)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(4))
                .setMarginBottom(6);
        right.add(invLabel);

        right.add(p(res.billId(), bold, 12f, G10).setMarginBottom(2));
        right.add(p(DATE_FMT.format(res.createdAt()), reg, FS_SMALL, G55));

        t.addCell(left);
        t.addCell(right);
        doc.add(t);

        // Blue rule under header
        doc.add(new Paragraph()
                .setBorderBottom(new SolidBorder(BLUE, 2f))
                .setMarginTop(8).setMarginBottom(0));
    }

    // ── 2. Billed-to + Payment ─────────────────────────────────────────────────

    private void billedToRow(Document doc, PdfFont bold, PdfFont reg,
                             BillConfirmResponse res) {

        Table t = new Table(UnitValue.createPercentArray(new float[]{54, 46}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);

        // BILLED TO
        Cell bt = new Cell()
                .setBorder(new SolidBorder(G85, 0.8f))
                .setBackgroundColor(BLUE_LIGHT)
                .setPadding(10).setMarginRight(8);

        bt.add(sectionLabel("BILLED TO").setMarginBottom(6));

        if (res.customer() != null) {
            var c = res.customer();
            labelVal(bt, bold, reg, "Name",  c.name());
            labelVal(bt, bold, reg, "Phone", c.phone());
            if (ok(c.email())) labelVal(bt, bold, reg, "Email", c.email());
        } else {
            bt.add(p("Walk-in Customer", reg, FS_BODY, G35));
        }

        // PAYMENT METHOD
        Cell pm = new Cell()
                .setBorder(new SolidBorder(G85, 0.8f))
                .setPadding(10);

        pm.add(sectionLabel("PAYMENT METHOD").setMarginBottom(6));
        pm.add(p(res.paymentMethod().toUpperCase(), bold, 13f, BLUE).setMarginBottom(3));

        if (res.gstEnabled()) {
            pm.add(labeledLine(bold, reg, "GST", "Applied"));
        }
        if (res.khataAmount().compareTo(BigDecimal.ZERO) > 0) {
            pm.add(labeledLine(bold, reg, "Collected", fmt(res.paidNow())));
            pm.add(labeledLine(bold, reg, "Khata", fmt(res.khataAmount())));
        } else {
            pm.add(p("Fully Paid", reg, FS_SMALL, GREEN_DARK));
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

        // Dynamic columns
        float[] cols = showDisc && showGst
                ? new float[]{4, 30, 10, 7, 12, 9, 9, 13}
                : showDisc
                ? new float[]{4, 33, 11, 8, 14, 10, 16}
                : showGst
                ? new float[]{4, 33, 11, 8, 14, 12, 16}
                : new float[]{4, 38, 13, 9, 18, 18};

        String[] heads = showDisc && showGst
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Discount", "GST%", "Amount"}
                : showDisc
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Discount", "Amount"}
                : showGst
                ? new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "GST%", "Amount"}
                : new String[]{"#", "Item", "SKU", "Qty", "Unit Price", "Amount"};

        TextAlignment[] aligns = buildAligns(heads.length);

        Table tbl = new Table(UnitValue.createPercentArray(cols))
                .setWidth(UnitValue.createPercentValue(100));

        // Light-blue header row with dark-blue text and blue bottom border
        for (int i = 0; i < heads.length; i++) {
            Cell hc = new Cell()
                    .setBackgroundColor(BLUE_LIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(BLUE, 1.5f))
                    .setPaddingTop(7).setPaddingBottom(7)
                    .setPaddingLeft(4).setPaddingRight(4);
            hc.add(new Paragraph(heads[i])
                    .setFont(bold).setFontSize(FS_LABEL + 0.5f)
                    .setFontColor(BLUE_NAVY).setTextAlignment(aligns[i]).setMargin(0));
            tbl.addHeaderCell(hc);
        }

        List<BillLineItemResponse> items = res.lineItems();
        for (int i = 0; i < items.size(); i++) {
            BillLineItemResponse it = items.get(i);
            var bg = (i % 2 == 1) ? G94 : WHITE;

            // Build cell values for this row
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
                boolean isAmt = j == vals.size() - 1;
                Cell dc = new Cell()
                        .setBackgroundColor(bg)
                        .setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(G85, 0.4f))
                        .setPaddingTop(5).setPaddingBottom(5)
                        .setPaddingLeft(4).setPaddingRight(4);
                dc.add(new Paragraph(vals.get(j))
                        .setFont(isAmt ? bold : reg)
                        .setFontSize(FS_BODY).setFontColor(G10)
                        .setTextAlignment(aligns[j]).setMargin(0));
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
        // Only show subtotal if it differs from grand total
        boolean showSub = hasDisc || hasGst;

        Table t = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(46))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT);

        if (showSub)
            sumRow(t, reg, bold, "Subtotal",   fmt(res.subtotal()),             false);
        if (hasDisc)
            sumRow(t, reg, bold, "Discount",   "- " + fmt(res.totalDiscount()), false);
        if (hasGst)
            sumRow(t, reg, bold, "GST",        "+ " + fmt(res.gstAmount()),     false);

        // Grand total — light-blue background, navy label, orange amount
        Cell gl = new Cell()
                .setBackgroundColor(BLUE_LIGHT)
                .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(BLUE, 1.5f))
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(7);
        gl.add(new Paragraph("GRAND TOTAL")
                .setFont(bold).setFontSize(FS_TOTAL).setFontColor(BLUE_NAVY).setMargin(0));

        Cell gr = new Cell()
                .setBackgroundColor(BLUE_LIGHT)
                .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(BLUE, 1.5f))
                .setPaddingTop(8).setPaddingBottom(8).setPaddingRight(7);
        gr.add(new Paragraph(fmt(res.grandTotal()))
                .setFont(bold).setFontSize(FS_TOTAL).setFontColor(ORANGE)
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0));

        t.addCell(gl);
        t.addCell(gr);
        doc.add(t);
    }

    private void sumRow(Table t, PdfFont reg, PdfFont bold,
                        String label, String value, boolean first) {
        SolidBorder b = new SolidBorder(G85, 0.5f);

        Cell lc = new Cell().setBorder(Border.NO_BORDER).setBorderTop(b).setPadding(5);
        lc.add(new Paragraph(label).setFont(reg).setFontSize(FS_BODY).setFontColor(G35).setMargin(0));

        Cell vc = new Cell().setBorder(Border.NO_BORDER).setBorderTop(b).setPadding(5);
        vc.add(new Paragraph(value).setFont(bold).setFontSize(FS_BODY).setFontColor(G10)
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
                .setBorder(new SolidBorder(G85, 0.8f));

        // Section header — light blue band
        Cell hdr = new Cell(1, 2)
                .setBackgroundColor(BLUE_LIGHT)
                .setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(G85, 0.5f))
                .setPaddingTop(6).setPaddingBottom(6).setPaddingLeft(10);
        hdr.add(new Paragraph("PAYMENT SUMMARY")
                .setFont(bold).setFontSize(FS_LABEL + 0.5f)
                .setFontColor(BLUE).setCharacterSpacing(0.5f).setMargin(0));
        box.addCell(hdr);

        payRow(box, reg, bold, "Payment Method",  res.paymentMethod().toUpperCase(), G10, false);
        payRow(box, reg, bold, "Amount Collected", fmt(res.paidNow()), GREEN_DARK, false);

        if (hasKhata) {
            payRow(box, reg, bold, "Deferred to Khata", fmt(res.khataAmount()), RED_DARK, false);
            // Total due = khataAmount from this bill (we don't show running balance as it changes)
            // Note for customer
            Cell note = new Cell(1, 2)
                    .setBorder(Border.NO_BORDER)
                    .setBorderTop(new SolidBorder(G85, 0.5f))
                    .setBackgroundColor(G94)
                    .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(10).setPaddingRight(10);
            note.add(new Paragraph(
                    "Note: " + fmt(res.khataAmount()) + " has been added to " +
                    (res.customer() != null ? res.customer().name() : "customer") +
                    "'s Khata book. Check Khata tab for current outstanding balance.")
                    .setFont(reg).setFontSize(FS_SMALL).setFontColor(G35)
                    .setItalic().setMargin(0));
            box.addCell(note);
        }

        doc.add(box);
    }

    private void payRow(Table t, PdfFont reg, PdfFont bold,
                        String label, String value, Object color, boolean last) {
        SolidBorder border = new SolidBorder(G85, 0.5f);

        Cell lc = new Cell()
                .setBorder(Border.NO_BORDER).setBorderTop(border)
                .setPaddingTop(7).setPaddingBottom(7).setPaddingLeft(10);
        lc.add(new Paragraph(label).setFont(reg).setFontSize(FS_BODY).setFontColor(G35).setMargin(0));

        Cell vc = new Cell()
                .setBorder(Border.NO_BORDER).setBorderTop(border)
                .setPaddingTop(7).setPaddingBottom(7).setPaddingRight(10);
        Paragraph vp = new Paragraph(value).setFont(bold).setFontSize(FS_BODY)
                .setTextAlignment(TextAlignment.RIGHT).setMargin(0);
        if (color instanceof DeviceRgb rgb)  vp.setFontColor(rgb);
        else if (color instanceof DeviceGray g) vp.setFontColor(g);
        vc.add(vp);

        t.addCell(lc);
        t.addCell(vc);
    }

    // ── 6. Footer — drawn directly onto last page canvas ─────────────────────

    private void drawFooter(PdfDocument pdf, PdfFont reg, BillPdfRequest shop, int lastPage) throws IOException {
        PdfPage page = pdf.getPage(lastPage);
        float w = page.getPageSize().getWidth();
        float y = 20f; // 20pt from bottom

        PdfCanvas canvas = new PdfCanvas(page);

        // Thin blue rule
        canvas.setStrokeColor(BLUE).setLineWidth(0.8f)
              .moveTo(MARGIN, y + 22).lineTo(w - MARGIN, y + 22).stroke();

        // Thank you text
        String name = ok(shop.shopName()) ? shop.shopName() : "our store";
        Rectangle rect = new Rectangle(MARGIN, y, w - 2 * MARGIN, 20f);
        try (Canvas c2 = new Canvas(canvas, rect)) {
            c2.add(new Paragraph("Thank you for shopping at " + name + "!  ·  Computer generated invoice — no signature required.")
                    .setFont(reg).setFontSize(FS_SMALL).setFontColor(G35)
                    .setTextAlignment(TextAlignment.CENTER).setMargin(0));
        }
        canvas.release();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Paragraph sectionLabel(String text) {
        return new Paragraph(text)
                .setFontSize(FS_LABEL)
                .setFontColor(BLUE).setCharacterSpacing(0.8f)
                .setBold().setMargin(0);
    }

    private void labelVal(Cell cell, PdfFont bold, PdfFont reg, String label, String value) {
        if (value == null || value.isBlank()) return;
        String pad = label + " ".repeat(Math.max(0, 7 - label.length()));
        Paragraph p = new Paragraph().setMarginBottom(2);
        p.add(new Text(pad).setFont(bold).setFontSize(FS_SMALL).setFontColor(G55));
        p.add(new Text(": " + value).setFont(reg).setFontSize(FS_BODY).setFontColor(G10));
        cell.add(p);
    }

    private Paragraph labeledLine(PdfFont bold, PdfFont reg, String label, String value) {
        Paragraph p = new Paragraph().setMarginBottom(2);
        p.add(new Text(label + ": ").setFont(bold).setFontSize(FS_SMALL).setFontColor(G55));
        p.add(new Text(value).setFont(reg).setFontSize(FS_BODY).setFontColor(G10));
        return p;
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
        if (ok(s.shopPhone2())) {
            if (!sb.isEmpty()) sb.append("  /  ");
            sb.append(s.shopPhone2());
        }
        return sb.toString();
    }

    private String fmt(BigDecimal a) {
        if (a == null) return "0.00";
        return "₹" + String.format("%,.2f", a);  // ₹
    }

    private TextAlignment[] buildAligns(int count) {
        TextAlignment[] a = new TextAlignment[count];
        a[0] = TextAlignment.CENTER;                  // #
        a[1] = TextAlignment.LEFT;                    // Item
        a[2] = TextAlignment.LEFT;                    // SKU
        a[3] = TextAlignment.CENTER;                  // Qty
        for (int i = 4; i < count; i++)
            a[i] = TextAlignment.RIGHT;               // prices / amount
        return a;
    }

}
