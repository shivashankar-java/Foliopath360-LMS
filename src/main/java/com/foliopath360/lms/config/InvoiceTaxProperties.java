package com.foliopath360.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "invoice.tax")
public class InvoiceTaxProperties {

    private BigDecimal defaultRate = BigDecimal.ZERO;

    private boolean inclusive = true;

    private String companyState = "Karnataka";

    private Set<UUID> exemptCourseIds = new HashSet<>();

    private Map<UUID, BigDecimal> ratesByCourse = new HashMap<>();

    public BigDecimal rateFor(UUID courseId) {
        if (courseId != null && exemptCourseIds.contains(courseId)) {
            return BigDecimal.ZERO;
        }
        if (courseId != null && ratesByCourse.containsKey(courseId)) {
            return ratesByCourse.get(courseId);
        }
        return defaultRate;
    }

    public boolean isInterState(String studentState) {
        if (studentState == null || studentState.isBlank()) {
            return false;
        }
        return !studentState.trim().equalsIgnoreCase(companyState == null ? "" : companyState.trim());
    }
}
