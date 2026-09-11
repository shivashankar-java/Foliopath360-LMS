package com.foliopath360.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mock_test_id", nullable = false)
    private MockTest mockTest;

    @Column(name = "question_code", nullable = false, unique = true, length = 50)
    private String questionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    @Builder.Default
    private QuestionType questionType = QuestionType.TEXT;

    @Column(name = "question_text", nullable = false, length = 2000)
    private String questionText;

    @Lob
    @Column(name = "code_content", columnDefinition = "LONGTEXT")
    private String codeContent;

    @Column(name = "code_language", length = 50)
    private String codeLanguage;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    @Lob
    @Column(name = "solution", columnDefinition = "LONGTEXT")
    private String solution;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(
            mappedBy = "question",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<MockTestOption> options = new ArrayList<>();
}
