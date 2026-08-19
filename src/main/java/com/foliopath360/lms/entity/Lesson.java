package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "lesson_code", nullable = false, unique = true, length = 50)
    private String lessonCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LessonStatus status = LessonStatus.DRAFT;
}
