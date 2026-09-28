package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A persisted coding submission for a {@link ProgrammingQuestion}.
 *
 * <p>Submissions are the source of truth for the per-student solved status
 * shown in the course list and for the LeetCode-style Submissions tab. Each
 * submission stores the exact code that was run plus the derived verdict so
 * history can be replayed without re-executing anything.</p>
 */
@Entity
@Table(
        name = "programming_question_submissions",
        indexes = {
                @Index(
                        name = "idx_pq_sub_user_question",
                        columnList = "user_id, question_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionSubmission extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ProgrammingQuestion question;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Lob
    @Column(name = "code", columnDefinition = "LONGTEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProgrammingSubmissionStatus status;

    @Column(name = "accepted", nullable = false)
    private Boolean accepted;

    @Column(name = "total_test_cases", nullable = false)
    private Integer totalTestCases;

    @Column(name = "passed_test_cases", nullable = false)
    private Integer passedTestCases;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    /**
     * Best-effort peak memory in kilobytes. May be {@code null} when the
     * local executor cannot measure the child process memory.
     */
    @Column(name = "memory_kb")
    private Long memoryKb;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
}
