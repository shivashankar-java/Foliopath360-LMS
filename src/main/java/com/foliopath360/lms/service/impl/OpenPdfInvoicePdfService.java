package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceItem;
import com.foliopath360.lms.entity.InvoiceTax;
import com.foliopath360.lms.exception.InvoiceGenerationException;
import com.foliopath360.lms.service.InvoicePdfService;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class OpenPdfInvoicePdfService implements InvoicePdfService {

    private static final String FONT_RESOURCE =
            "net/sf/jasperreports/fonts/dejavu/DejaVuSans.ttf";
    private static final String BOLD_FONT_RESOURCE =
            "net/sf/jasperreports/fonts/dejavu/DejaVuSans-Bold.ttf";

    private static final Color BRAND_DARK = new Color(8, 53, 43);
    private static final Color BRAND_GREEN = new Color(16, 126, 95);
    private static final Color BRAND_GOLD = new Color(232, 178, 62);
    private static final Color LIGHT_GRAY = new Color(245, 247, 246);
    private static final Color BORDER = new Color(215, 224, 220);
    private static final Color MUTED = new Color(90, 105, 100);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final InvoiceProperties properties;
    private final BaseFont regularFont;
    private final BaseFont boldFont;

    public OpenPdfInvoicePdfService(InvoiceProperties properties) {
        this.properties = properties;
        this.regularFont = loadFont(properties.getFontPath(), FONT_RESOURCE);
        this.boldFont = loadFont(properties.getFontPath(), BOLD_FONT_RESOURCE);
    }

    @Override
    public byte[] generate(Invoice invoice) {
        if (invoice == null || invoice.getInvoiceNumber() == null) {
            throw new InvoiceGenerationException("Invoice data is incomplete");
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 38, 38, 42, 62);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new InvoicePageEvent(
                    properties.getCompany().getName(),
                    properties.getCompany().getTagline(),
                    boldFont,
                    regularFont
            ));

            document.open();
            addHeader(document, invoice);
            addParties(document, invoice);
            addPaymentDetails(document, invoice);
            addChargeTable(document, invoice);
            addPaymentSummary(document, invoice);
            addFooterNotes(document);
            document.close();

            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new InvoiceGenerationException("Unable to generate invoice PDF", ex);
        }
    }

    private void addHeader(Document document, Invoice invoice) throws DocumentException {
        Font brandFont = font(boldFont, 19, Color.WHITE);
        Font whiteSmall = font(regularFont, 8.5f, Color.WHITE);
        Font taglineFont = font(regularFont, 9, BRAND_GREEN);

        PdfPTable banner = new PdfPTable(new float[]{3.2f, 1.8f});
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(10);

        PdfPCell brandCell = new PdfPCell();
        brandCell.setBackgroundColor(BRAND_DARK);
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.setPaddingLeft(16);
        brandCell.setPaddingTop(12);
        brandCell.setPaddingBottom(12);
        brandCell.addElement(new Phrase("FOLIOPATH 360", brandFont));
        brandCell.addElement(new Phrase("Learn. Grow. Go Beyond.", whiteSmall));
        banner.addCell(brandCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBackgroundColor(BRAND_DARK);
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setPaddingRight(16);
        titleCell.setPaddingTop(12);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(new Phrase("TAX INVOICE", font(boldFont, 14, BRAND_GOLD)));
        titleCell.addElement(new Phrase(invoice.getInvoiceNumber(), whiteSmall));
        banner.addCell(titleCell);

        document.add(banner);

        PdfPTable intro = new PdfPTable(new float[]{3.2f, 1.8f});
        intro.setWidthPercentage(100);
        intro.setSpacingAfter(8);

        PdfPCell company = new PdfPCell();
        company.setBorder(Rectangle.NO_BORDER);
        company.setPaddingLeft(12);
        InvoiceProperties.Company co = properties.getCompany();
        company.addElement(new Phrase(co.getLegalName(), font(boldFont, 10.5f, BRAND_DARK)));
        company.addElement(new Phrase(co.getAddressLine1(), font(regularFont, 8.5f, MUTED)));
        company.addElement(new Phrase(co.getAddressLine2(), font(regularFont, 8.5f, MUTED)));
        company.addElement(new Phrase("Support: " + co.getSupportEmail()
                + "  |  " + co.getContactNumber(), font(regularFont, 8.5f, MUTED)));
        company.addElement(new Phrase(co.getWebsite()
                + (co.getGstin() == null || co.getGstin().isBlank()
                    ? "" : "  |  GSTIN: " + co.getGstin()), font(regularFont, 8.5f, MUTED)));
        intro.addCell(company);

        PdfPCell summary = new PdfPCell();
        summary.setBorder(Rectangle.NO_BORDER);
        summary.setBackgroundColor(LIGHT_GRAY);
        summary.setPadding(10);
        summary.addElement(labelValue("Invoice Date",
                DATE_FORMATTER.format(invoice.getInvoiceDate()), false));
        summary.addElement(labelValue("Payment Date",
                DATE_FORMATTER.format(invoice.getPaymentDate()), false));
        summary.addElement(labelValue("Status",
                invoice.getPaymentStatus(), false));
        intro.addCell(summary);

        document.add(intro);
        document.add(new Phrase(co.getTagline(), taglineFont));
    }

    private void addParties(Document document, Invoice invoice) throws DocumentException {
        sectionTitle(document, "BILLED TO");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);

        PdfPCell student = new PdfPCell();
        student.setBorder(Rectangle.BOX);
        student.setBorderColor(BORDER);
        student.setPadding(10);
        student.addElement(new Phrase(invoice.getStudentName(),
                font(boldFont, 10.5f, BRAND_DARK)));
        student.addElement(new Phrase("Student ID: " + nullToEmpty(invoice.getStudentCode()),
                font(regularFont, 9, MUTED)));
        student.addElement(new Phrase("Email: " + invoice.getStudentEmail(),
                font(regularFont, 9, MUTED)));
        student.addElement(new Phrase("Phone: " + nullToEmpty(invoice.getStudentPhone()),
                font(regularFont, 9, MUTED)));
        student.addElement(new Phrase("Address: " + nullToEmpty(invoice.getBillingAddress()),
                font(regularFont, 9, MUTED)));
        if (invoice.getStudentGstin() != null && !invoice.getStudentGstin().isBlank()) {
            student.addElement(new Phrase("GSTIN: " + invoice.getStudentGstin(),
                    font(regularFont, 9, MUTED)));
        }
        table.addCell(student);

        PdfPCell course = new PdfPCell();
        course.setBorder(Rectangle.BOX);
        course.setBorderColor(BORDER);
        course.setPadding(10);
        course.addElement(new Phrase("PURCHASE REFERENCE",
                font(boldFont, 9, BRAND_GREEN)));
        course.addElement(new Phrase("Course: " + nullToEmpty(invoice.getCourseName()),
                font(regularFont, 9, BRAND_DARK)));
        course.addElement(new Phrase("Order No: " + invoice.getOrder().getOrderNumber(),
                font(regularFont, 9, MUTED)));
        course.addElement(new Phrase("Payment Method: "
                + nullToEmpty(invoice.getPaymentMethod()), font(regularFont, 9, MUTED)));
        course.addElement(new Phrase("Transaction ID: "
                + nullToEmpty(invoice.getPaymentReference()), font(regularFont, 9, MUTED)));
        table.addCell(course);

        document.add(table);
    }

    private void addPaymentDetails(Document document, Invoice invoice) throws DocumentException {
        sectionTitle(document, "PAYMENT & GATEWAY DETAILS");

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{1.2f, 1.6f, 1.2f, 1.6f});

        addDetailCell(table, "Payment Status", invoice.getPaymentStatus());
        addDetailCell(table, "Payment Method", invoice.getPaymentMethod());
        addDetailCell(table, "Transaction ID", invoice.getPaymentReference());
        addDetailCell(table, "Payment Date",
                DATE_FORMATTER.format(invoice.getPaymentDate()));
        addDetailCell(table, "Razorpay Payment ID", invoice.getRazorpayPaymentId());
        addDetailCell(table, "Razorpay Order ID", invoice.getRazorpayOrderId());
        addDetailCell(table, "Currency", invoice.getCurrency());
        addDetailCell(table, "Invoice Number", invoice.getInvoiceNumber());

        document.add(table);
    }

    private void addChargeTable(Document document, Invoice invoice) throws DocumentException {
        sectionTitle(document, "CHARGES & TAX BREAKDOWN");

        PdfPTable table = new PdfPTable(new float[]{4f, 1f, 1.8f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(12);

        addHeaderCell(table, "Description");
        addHeaderCell(table, "Qty");
        addHeaderCell(table, "Amount", Element.ALIGN_RIGHT);

        for (InvoiceItem item : invoice.getItems()) {
            addDataCell(table, item.getDescription());
            addDataCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER);
            addDataCell(table, money(item.getUnitPrice()), Element.ALIGN_RIGHT);
        }

        addAmountRow(table, "Discount", invoice.getDiscountAmount(), false);
        addAmountRow(table, "Coupon Discount", invoice.getCouponDiscountAmount(), false);
        addAmountRow(table, "Scholarship / Waiver", invoice.getScholarshipAmount(), false);
        addAmountRow(table, "Taxable Amount", invoice.getTaxableAmount(), false);

        for (InvoiceTax tax : invoice.getTaxes()) {
            addAmountRow(table, tax.getTaxName() + " ("
                    + percent(tax.getTaxRate()) + "%)", tax.getTaxAmount(), false);
        }

        addAmountRow(table, "GRAND TOTAL", invoice.getTotalAmount(), true);
        document.add(table);
    }

    private void addPaymentSummary(Document document, Invoice invoice) throws DocumentException {
        sectionTitle(document, "PAYMENT SUMMARY");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);

        addSummaryCell(table, "Total Course Price", money(invoice.getSubtotal()));
        addSummaryCell(table, "Discount", money(invoice.getDiscountAmount()));
        addSummaryCell(table, "Coupon Discount", money(invoice.getCouponDiscountAmount()));
        addSummaryCell(table, "Scholarship / Waiver", money(invoice.getScholarshipAmount()));
        addSummaryCell(table, "Tax Amount", money(invoice.getTotalTax()));
        addSummaryCell(table, "Final Paid Amount", money(invoice.getTotalAmount()));
        addSummaryCell(table, "Payment Mode", nullToEmpty(invoice.getPaymentMethod()));
        addSummaryCell(table, "Payment Reference", nullToEmpty(invoice.getPaymentReference()));

        document.add(table);
    }

    private void addFooterNotes(Document document) throws DocumentException {
        PdfPTable notes = new PdfPTable(1);
        notes.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setPadding(10);
        cell.addElement(new Phrase("Thank you for choosing Foliopath 360!",
                font(boldFont, 10, BRAND_DARK)));
        cell.addElement(new Phrase("Learn. Grow. Go Beyond.",
                font(regularFont, 9, BRAND_GREEN)));
        cell.addElement(new Phrase("Terms: " + properties.getCompany().getTerms(),
                font(regularFont, 8, MUTED)));
        cell.addElement(new Phrase("Refund Policy: "
                + properties.getCompany().getRefundPolicy(), font(regularFont, 8, MUTED)));
        cell.addElement(new Phrase("This is a computer-generated invoice and does not "
                + "require a physical signature.", font(regularFont, 8, MUTED)));
        cell.addElement(new Phrase("Need help? " + properties.getCompany().getSupportEmail()
                + " | " + properties.getCompany().getContactNumber(),
                font(regularFont, 8, MUTED)));
        notes.addCell(cell);
        document.add(notes);
    }

    private void sectionTitle(Document document, String title) throws DocumentException {
        document.add(new Phrase(title, font(boldFont, 10, BRAND_GREEN)));
    }

    private void addDetailCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Phrase(label, font(regularFont, 7.5f, MUTED)));
        cell.addElement(new Phrase(nullToEmpty(value), font(boldFont, 8.5f, BRAND_DARK)));
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        addHeaderCell(table, text, Element.ALIGN_LEFT);
    }

    private void addHeaderCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(boldFont, 9, Color.WHITE)));
        cell.setBackgroundColor(BRAND_DARK);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        addDataCell(table, text, Element.ALIGN_LEFT);
    }

    private void addDataCell(PdfPTable table, String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(nullToEmpty(text),
                font(regularFont, 9, BRAND_DARK)));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private void addAmountRow(PdfPTable table, String label, BigDecimal amount,
                              boolean highlight) {
        Font font = font(highlight ? boldFont : regularFont,
                highlight ? 10 : 9, highlight ? Color.WHITE : BRAND_DARK);
        PdfPCell description = new PdfPCell(new Phrase(label, font));
        PdfPCell quantity = new PdfPCell(new Phrase("", font));
        PdfPCell value = new PdfPCell(new Phrase(money(amount), font));
        for (PdfPCell cell : new PdfPCell[]{description, quantity, value}) {
            cell.setBorder(highlight ? Rectangle.NO_BORDER : Rectangle.BOTTOM);
            cell.setBorderColor(BORDER);
            cell.setPadding(7);
            cell.setBackgroundColor(highlight ? BRAND_DARK : Color.WHITE);
        }
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(description);
        table.addCell(quantity);
        table.addCell(value);
    }

    private void addSummaryCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Phrase(label, font(regularFont, 8, MUTED)));
        cell.addElement(new Phrase(value, font(boldFont, 9, BRAND_DARK)));
        table.addCell(cell);
    }

    private Phrase labelValue(String label, String value, boolean bold) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(label + "\n", font(regularFont, 7.5f, MUTED)));
        phrase.add(new Phrase(value, font(bold ? boldFont : regularFont, 9, BRAND_DARK)));
        return phrase;
    }

    private Font font(BaseFont baseFont, float size, Color color) {
        return new Font(baseFont, size, Font.NORMAL, color);
    }

    private String money(BigDecimal amount) {
        NumberFormat format = NumberFormat.getInstance(new Locale("en", "IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "₹ " + format.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String percent(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return rate.stripTrailingZeros().toPlainString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private BaseFont loadFont(String configuredPath, String classpathResource) {
        String fontName = classpathResource;
        try {
            if (configuredPath != null && !configuredPath.isBlank()
                    && !configuredPath.contains("${")) {
                Path path = Paths.get(configuredPath);
                if (Files.exists(path)) {
                    fontName = path.toAbsolutePath().toString();
                }
            }
            return BaseFont.createFont(fontName, BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, false);
        } catch (DocumentException | IOException ex) {
            throw new InvoiceGenerationException(
                    "Unable to load Unicode font for invoice PDF", ex);
        }
    }

    private static class InvoicePageEvent extends PdfPageEventHelper {

        private final String companyName;
        private final String tagline;
        private final BaseFont boldFont;
        private final BaseFont regularFont;

        InvoicePageEvent(String companyName, String tagline,
                         BaseFont boldFont, BaseFont regularFont) {
            this.companyName = companyName;
            this.tagline = tagline;
            this.boldFont = boldFont;
            this.regularFont = regularFont;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            canvas.setColorStroke(BORDER);
            canvas.moveTo(document.leftMargin(), 52);
            canvas.lineTo(page.getWidth() - document.rightMargin(), 52);
            canvas.stroke();

            canvas.beginText();
            canvas.setColorFill(MUTED);
            canvas.setFontAndSize(regularFont, 7.5f);
            canvas.showTextAligned(Element.ALIGN_LEFT,
                    companyName + "  |  " + tagline,
                    document.leftMargin(), 40, 0);
            canvas.setFontAndSize(boldFont, 7.5f);
            canvas.showTextAligned(Element.ALIGN_RIGHT,
                    "Page " + writer.getPageNumber(),
                    page.getWidth() - document.rightMargin(), 40, 0);
            canvas.endText();

            if (writer.getPageNumber() > 1) {
                canvas.beginText();
                canvas.setColorFill(BRAND_GREEN);
                canvas.setFontAndSize(boldFont, 8);
                canvas.showTextAligned(Element.ALIGN_LEFT,
                        "FOLIOPATH 360 - TAX INVOICE",
                        document.leftMargin(), page.getHeight() - 26, 0);
                canvas.endText();
            }
        }
    }
}
