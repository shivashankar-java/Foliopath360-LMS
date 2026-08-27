package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "interview_kit_enrollments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kit_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitEnrollment extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kit_id", nullable = false)
    private InterviewKit kit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private KitEnrollmentStatus status = KitEnrollmentStatus.ENROLLED;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;
}
