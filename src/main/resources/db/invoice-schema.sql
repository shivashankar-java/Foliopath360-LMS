-- Foliopath 360 invoice module schema (MySQL 8+).
-- Hibernate ddl-auto=update creates these automatically in development.
-- Apply this script explicitly in production via your migration tool.

-- Offline payment references are stored on the existing payments table.
ALTER TABLE payments ADD COLUMN payment_reference VARCHAR(120) NULL;

CREATE TABLE IF NOT EXISTS invoice_sequences (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id CHAR(36) NOT NULL,
    invoice_number VARCHAR(40) NOT NULL,
    student_id CHAR(36) NOT NULL,
    order_id CHAR(36) NOT NULL,
    payment_id CHAR(36) NOT NULL,
    course_id CHAR(36) NULL,
    course_name VARCHAR(250) NULL,
    invoice_date DATETIME(6) NOT NULL,
    payment_date DATETIME(6) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    coupon_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    scholarship_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    taxable_amount DECIMAL(12,2) NOT NULL,
    cgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    sgst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    igst_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_tax DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_method VARCHAR(50) NULL,
    payment_status VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(120) NULL,
    razorpay_order_id VARCHAR(100) NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    student_name VARCHAR(200) NOT NULL,
    student_code VARCHAR(50) NULL,
    student_email VARCHAR(150) NOT NULL,
    student_phone VARCHAR(30) NULL,
    student_gstin VARCHAR(20) NULL,
    billing_address VARCHAR(500) NULL,
    pdf_storage_key VARCHAR(500) NULL,
    pdf_content_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    email_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_attempts INT NOT NULL DEFAULT 0,
    email_sent_at DATETIME(6) NULL,
    last_email_attempt_at DATETIME(6) NULL,
    last_email_error VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    created_dt DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_dt DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_invoices_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_invoices_course FOREIGN KEY (course_id) REFERENCES courses (id),
    INDEX idx_invoices_student_id (student_id),
    INDEX idx_invoices_course_id (course_id),
    INDEX idx_invoices_invoice_date (invoice_date),
    INDEX idx_invoices_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_items (
    id CHAR(36) NOT NULL,
    invoice_id CHAR(36) NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    line_number INT NOT NULL,
    description VARCHAR(250) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    taxable_amount DECIMAL(12,2) NOT NULL,
    tax_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(12,2) NOT NULL,
    created_by BIGINT NULL,
    created_dt DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_dt DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_taxes (
    id CHAR(36) NOT NULL,
    invoice_id CHAR(36) NOT NULL,
    tax_code VARCHAR(10) NOT NULL,
    tax_name VARCHAR(100) NOT NULL,
    tax_rate DECIMAL(6,4) NOT NULL,
    tax_amount DECIMAL(12,2) NOT NULL,
    created_by BIGINT NULL,
    created_dt DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_dt DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_invoice_taxes_invoice_code UNIQUE (invoice_id, tax_code),
    CONSTRAINT fk_invoice_taxes_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_events (
    id CHAR(36) NOT NULL,
    invoice_id CHAR(36) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    message VARCHAR(500) NULL,
    actor_email VARCHAR(150) NULL,
    created_by BIGINT NULL,
    created_dt DATETIME(6) NULL,
    updated_by BIGINT NULL,
    updated_dt DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_events_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    INDEX idx_invoice_events_invoice_id (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
