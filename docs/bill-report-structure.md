# Bill Report — Structure & Data Reference

## Overview

A **Bill** is the primary sales document generated at the MKR Commerce POS. It records a completed retail transaction including the customer, items purchased, pricing, GST, payment method, and any khata (credit) entry.

Bills are identified by a human-readable ID (`MKR-BILL-0001`) and an internal UUID.

---

## Bill Sections (printed / PDF layout)

```
┌────────────────────────────────────────────────────────┐
│  SECTION 1 — SHOP HEADER                              │
│  Shop Name (large, bold)                              │
│  Tagline · Address · Phone · Email · GSTIN            │
├──────────────────────┬─────────────────────────────────┤
│  SECTION 2 — BILL    │  SECTION 3 — CUSTOMER           │
│  Bill No: MKR-BILL-  │  Name, Phone, Email             │
│  Date & Time         │  Customer ID (CUS-001)          │
│  Payment Method      │  Khata Balance (if any)         │
├──────────────────────┴─────────────────────────────────┤
│  SECTION 4 — LINE ITEMS TABLE                         │
│  # │ Item Name      │ SKU  │ Qty │ Rate │ Disc │ GST% │ Total │
│  1 │ Samsung TV 32" │ SAM- │  1  │ 5000 │    - │  18% │ 5900  │
│  2 │ HDMI Cable     │ HDM- │  2  │  150 │   20 │   5% │  294  │
├────────────────────────────────────────────────────────┤
│  SECTION 5 — SUMMARY                                  │
│                    Subtotal:   ₹ 5,300.00             │
│                    Discount:  -₹    20.00             │
│                    GST:        ₹ 1,062.00             │
│                   ─────────────────────────            │
│                   GRAND TOTAL: ₹ 6,342.00             │
├────────────────────────────────────────────────────────┤
│  SECTION 6 — PAYMENT                                  │
│  [Paid Now: ₹5,342]  [Khata: ₹1,000]  [Total: ₹6,342]│
├────────────────────────────────────────────────────────┤
│  SECTION 7 — FOOTER                                   │
│  "Thank you for shopping at MKR Store! 🙏"           │
│  "Computer generated invoice — no signature required" │
└────────────────────────────────────────────────────────┘
```

---

## Data Fields Reference

### Shop (from frontend `ShopSettings` — localStorage)

| Field         | Type   | Description                          |
|---------------|--------|--------------------------------------|
| `shopName`    | String | Store display name                   |
| `shopTagline` | String | Subtitle / business description      |
| `shopAddress` | String | Full address                         |
| `shopPhone`   | String | Primary phone number                 |
| `shopPhone2`  | String | Secondary phone number (optional)    |
| `shopEmail`   | String | Contact email                        |
| `shopGstin`   | String | GST registration number (optional)   |

### Bill (from `bills` table / `BillConfirmResponse`)

| Field            | Type        | DB Column       | Description                                 |
|------------------|-------------|-----------------|---------------------------------------------|
| `id`             | UUID        | `id`            | Internal primary key                        |
| `billNumber`     | Long        | `bill_number`   | Sequential number (1001, 1002…)             |
| `billId`         | String      | computed        | Display format: `MKR-BILL-0001`             |
| `createdAt`      | Instant     | `created_at`    | Timestamp of bill creation                  |
| `gstEnabled`     | boolean     | `gst_enabled`   | Whether GST was applied to this bill        |
| `paymentMethod`  | String      | `payment_method`| `cash`, `upi`, `card`                       |
| `subtotal`       | BigDecimal  | `subtotal`      | Sum of (unitPrice × qty) before discounts   |
| `totalDiscount`  | BigDecimal  | `total_discount`| Sum of all line-item discounts              |
| `gstAmount`      | BigDecimal  | `gst_amount`    | Total GST charged                           |
| `grandTotal`     | BigDecimal  | `grand_total`   | subtotal − discount + gst                  |
| `paidNow`        | BigDecimal  | `paid_now`      | Amount collected immediately                |
| `khataAmount`    | BigDecimal  | `khata_amount`  | Amount deferred to customer's khata ledger  |

### Customer (from `customers` table / `BillCustomerDto`)

| Field           | Type       | DB Column        | Description                            |
|-----------------|------------|------------------|----------------------------------------|
| `id`            | UUID       | `id`             | Internal primary key                   |
| `customerId`    | String     | computed         | Display: `CUS-001`                     |
| `name`          | String     | `name`           | Full name                              |
| `phone`         | String     | `phone`          | Mobile number (unique)                 |
| `email`         | String     | `email`          | Email address (nullable)               |
| `pendingAmount` | BigDecimal | `pending_amount` | Total outstanding khata balance        |

> **Note:** The `bills` table also stores `customer_name` and `customer_phone` as **denormalized** columns for historical accuracy — so the bill remains correct even if the customer record is later deleted or updated.

### Line Items (from `bill_line_items` table / `BillLineItemResponse`)

| Field         | Type       | DB Column       | Description                                     |
|---------------|------------|-----------------|-------------------------------------------------|
| `productName` | String     | `product_name`  | Product name at time of billing (immutable)     |
| `productSku`  | String     | `product_sku`   | SKU at time of billing (immutable)              |
| `qty`         | int        | `qty`           | Quantity sold                                   |
| `unitPrice`   | BigDecimal | `unit_price`    | Price per unit as billed                        |
| `discount`    | BigDecimal | `discount`      | Flat discount applied to this line              |
| `gstPercent`  | BigDecimal | `gst_percent`   | GST rate applied (e.g. 18.00)                   |
| `gstAmount`   | BigDecimal | `gst_amount`    | GST charged on this line                        |
| `lineTotal`   | BigDecimal | `line_total`    | (unitPrice × qty − discount) + gstAmount        |

---

## Amount Calculation Logic

```
lineNet      = unitPrice × qty − discount
gstAmount    = lineNet × gstPercent / 100
lineTotal    = lineNet + gstAmount

subtotal     = Σ (unitPrice × qty)          ← before discounts
totalDiscount= Σ (discount per line)
gstAmount    = Σ (gstAmount per line)
grandTotal   = subtotal − totalDiscount + gstAmount

paidNow      = grandTotal − khataAmount
```

---

## Khata (Credit Ledger) Integration

When `khataAmount > 0`:
1. A `KhataEntry` record is created for the customer (type `AUTO_ORDER`)
2. `Customer.pendingAmount` is increased by `khataAmount`
3. The entry description references the bill ID: `"Bill MKR-BILL-0001"`

---

## PDF Generation

Two implementations are available:

### iText 7 (`ITextBillPdfService`)
- Pure Java, no external tools
- Generates A4 formatted invoice with branding
- Endpoint: `GET /api/billing/{uuid}/pdf`
- Shop settings passed as query parameters

### BIRT (`BirtBillPdfService`)
- Uses Eclipse BIRT 4.15 report engine
- Loads design from `src/main/resources/reports/bill-report.rptdesign`
- Same endpoint with `?engine=birt`
- `.rptdesign` can be opened and edited in Eclipse BIRT Designer plugin
- Data passed via BIRT `appContext` map: keys `"bill"` and `"shop"`

---

## Email Delivery (`BillEmailService`)

- Spring Boot Mail (SMTP)
- Sends HTML body with bill summary + PDF attachment
- Configure: `MAIL_USERNAME` / `MAIL_PASSWORD` environment variables (Gmail App Password)
- Endpoint: `POST /api/billing/{uuid}/send-email`
- Request body: shop settings + optional `toEmail` override

---

## WhatsApp Share

Frontend-only — opens `https://wa.me/91{phone}?text={message}` in a new tab. No backend call. The message includes bill ID, amount, and shop name.

---

## API Endpoints

| Method | Path                          | Description                              |
|--------|-------------------------------|------------------------------------------|
| POST   | `/api/billing/confirm`        | Create a new bill                        |
| GET    | `/api/billing`                | List bills (paginated, searchable)       |
| GET    | `/api/billing/{id}`           | Get bill by UUID                         |
| GET    | `/api/billing/{id}/pdf`       | Download bill as PDF (`?engine=itext\|birt`) |
| POST   | `/api/billing/{id}/send-email`| Send bill PDF to customer email          |

---

## Database Tables

```
bills
  id (UUID PK)
  bill_number (UNIQUE)
  customer_id (FK → customers, nullable)
  customer_name (denormalized)
  customer_phone (denormalized)
  gst_enabled
  payment_method
  subtotal / total_discount / gst_amount / grand_total / paid_now / khata_amount
  created_at / updated_at

bill_line_items
  id (UUID PK)
  bill_id (FK → bills, NOT NULL)
  product_id (FK → products, nullable — product may be deleted later)
  product_name / product_sku (denormalized)
  qty / unit_price / discount / gst_percent / gst_amount / line_total
  created_at / updated_at
```

---

## BIRT .rptdesign File Location

```
mkr-commerce-backend/
  src/
    main/
      resources/
        reports/
          bill-report.rptdesign   ← open in Eclipse with BIRT Designer plugin
```

To edit the report visually: install **Eclipse IDE for Java EE** + **BIRT Report Design** plugin from Eclipse Marketplace, then open the `.rptdesign` file.
