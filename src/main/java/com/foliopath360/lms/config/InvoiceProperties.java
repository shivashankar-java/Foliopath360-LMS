package com.foliopath360.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "invoice")
public class InvoiceProperties {

    private String numberPrefix = "INV";

    private String storagePath = "${java.io.tmpdir}/foliopath360/invoices";

    private String fontPath = "";

    private String invoiceLinkBaseUrl = "http://localhost:5173/student/invoices";

    private Company company = new Company();

    @Getter
    @Setter
    public static class Company {
        private String name = "Foliopath 360";
        private String legalName = "Foliopath 360 Technologies Private Limited";
        private String tagline = "Learn. Grow. Go Beyond.";
        private String addressLine1 = "Plot 12, Tech Park Road";
        private String addressLine2 = "Bengaluru, Karnataka 560103, India";
        private String supportEmail = "support@foliopath360.com";
        private String contactNumber = "+91 1800 123 4567";
        private String website = "https://www.foliopath360.com";
        private String gstin = "";
        private String state = "Karnataka";
        private String terms = "Payment is non-transferable. Access to purchased learning content "
                + "begins on successful payment and continues for the enrolled batch validity period.";
        private String refundPolicy = "Refunds, if any, are governed by the Foliopath 360 Refund Policy "
                + "published at https://www.foliopath360.com/refund-policy.";
    }
}
