package com.foliopath360.lms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTaxCalculationTest {

    @Test
    void calculatesInclusiveIntraStateGst() {
        InvoiceTaxCalculation result = InvoiceTaxCalculation.calculate(
                new BigDecimal("11800.00"), BigDecimal.ZERO,
                new BigDecimal("18"), false, true);

        assertThat(result.taxableAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.cgstAmount()).isEqualByComparingTo("900.00");
        assertThat(result.sgstAmount()).isEqualByComparingTo("900.00");
        assertThat(result.igstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("11800.00");
    }

    @Test
    void calculatesInclusiveInterStateGst() {
        InvoiceTaxCalculation result = InvoiceTaxCalculation.calculate(
                new BigDecimal("11800.00"), BigDecimal.ZERO,
                new BigDecimal("18"), true, true);

        assertThat(result.taxableAmount()).isEqualByComparingTo("10000.00");
        assertThat(result.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.sgstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.igstAmount()).isEqualByComparingTo("1800.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("11800.00");
    }

    @Test
    void calculatesExclusiveGstAfterDiscount() {
        InvoiceTaxCalculation result = InvoiceTaxCalculation.calculate(
                new BigDecimal("1000.00"), new BigDecimal("100.00"),
                new BigDecimal("18"), false, false);

        assertThat(result.taxableAmount()).isEqualByComparingTo("900.00");
        assertThat(result.totalTax()).isEqualByComparingTo("162.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("1062.00");
    }

    @Test
    void supportsGstExemption() {
        InvoiceTaxCalculation result = InvoiceTaxCalculation.calculate(
                new BigDecimal("5000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, false, true);

        assertThat(result.totalTax()).isEqualByComparingTo("0.00");
        assertThat(result.totalAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void rejectsInvalidTaxRate() {
        assertThatThrownBy(() -> InvoiceTaxCalculation.calculate(
                BigDecimal.TEN, BigDecimal.ZERO,
                new BigDecimal("150"), false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
