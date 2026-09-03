package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "course_id")
    private Course course;

    // Snapshot of the course title at purchase time
    @Column(name = "course_title", length = 200)
    private String courseTitle;

    // Optional interview kit for kit purchases (mutually exclusive with course)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "kit_id")
    private InterviewKit kit;

    // Snapshot of the kit name at purchase time for kit items
    @Column(name = "kit_name", length = 200)
    private String kitName;

    // Price snapshot used for this order (validated against live price at checkout)
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
