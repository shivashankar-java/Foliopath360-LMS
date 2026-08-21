package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonItem extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(name = "code_content", columnDefinition = "LONGTEXT")
    private String codeContent;

    @Column(name = "code_language", length = 50)
    private String codeLanguage;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
