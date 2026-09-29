package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "programming_questions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"question_code"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestion extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_code", nullable = false, unique = true, length = 50)
    private String questionCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "problem_statement", columnDefinition = "LONGTEXT")
    private String problemStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    @Builder.Default
    private ProgrammingQuestionDifficulty difficulty = ProgrammingQuestionDifficulty.MEDIUM;

    @Column(name = "allowed_languages", length = 100)
    private String allowedLanguages;

    @Lob
    @Column(name = "input_format", columnDefinition = "LONGTEXT")
    private String inputFormat;

    @Lob
    @Column(name = "output_format", columnDefinition = "LONGTEXT")
    private String outputFormat;

    @Lob
    @Column(name = "constraints_text", columnDefinition = "LONGTEXT")
    private String constraints;

    @Lob
    @Column(name = "sample_input", columnDefinition = "LONGTEXT")
    private String sampleInput;

    @Lob
    @Column(name = "sample_output", columnDefinition = "LONGTEXT")
    private String sampleOutput;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ProgrammingTestCase> testCases = new ArrayList<>();
}