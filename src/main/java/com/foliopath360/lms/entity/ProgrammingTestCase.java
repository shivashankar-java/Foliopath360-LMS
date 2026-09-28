package com.foliopath360.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "programming_test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ProgrammingQuestion question;

    @Lob
    @Column(name = "input", columnDefinition = "LONGTEXT")
    private String input;

    @Lob
    @Column(name = "expected_output", columnDefinition = "LONGTEXT")
    private String expectedOutput;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}