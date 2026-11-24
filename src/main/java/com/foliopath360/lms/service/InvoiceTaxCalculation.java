package com.foliopath360.lms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record InvoiceTaxCalculation(
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal taxableAmount,
        BigDecimal taxRate,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal igstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalTax,
        BigDecimal totalAmount,
        boolean interState
) {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public static InvoiceTaxCalculation calculate(
            BigDecimal subtotal,
            BigDecimal totalDiscount,
            BigDecimal taxRate,
            boolean interState,
            boolean inclusive
    ) {
        BigDecimal net = money(subtotal).subtract(money(totalDiscount)).max(BigDecimal.ZERO);
        BigDecimal rate = money(taxRate).max(BigDecimal.ZERO);

        if (rate.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Tax rate cannot exceed 100 percent");
        }

        BigDecimal taxable;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;

        if (rate.compareTo(BigDecimal.ZERO) == 0) {
            taxable = net;
        } else if (inclusive) {
            taxable = net.divide(BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP)),
                    2, RoundingMode.HALF_UP);
            BigDecimal tax = net.subtract(taxable);
            if (interState) {
                igstAmount = tax;
            } else {
                cgstAmount = tax.divide(TWO, 2, RoundingMode.HALF_UP);
                sgstAmount = tax.subtract(cgstAmount);
            }
        } else {
            taxable = net;
            BigDecimal tax = taxable.multiply(rate)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            if (interState) {
                igstAmount = tax;
            } else {
                cgstAmount = tax.divide(TWO, 2, RoundingMode.HALF_UP);
                sgstAmount = tax.subtract(cgstAmount);
            }
        }

        BigDecimal totalTax = cgstAmount.add(sgstAmount).add(igstAmount);
        BigDecimal total = taxable.add(totalTax);

        BigDecimal halfRate = rate.divide(TWO, 4, RoundingMode.HALF_UP);
        return new InvoiceTaxCalculation(
                money(subtotal),
                money(totalDiscount),
                taxable,
                rate,
                interState ? BigDecimal.ZERO : halfRate,
                interState ? BigDecimal.ZERO : halfRate,
                interState ? rate : BigDecimal.ZERO,
                cgstAmount,
                sgstAmount,
                igstAmount,
                totalTax,
                total,
                interState
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
