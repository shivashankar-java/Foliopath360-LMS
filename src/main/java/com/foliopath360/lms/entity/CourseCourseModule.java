package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "course_course_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCourseModule extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_course_id", nullable = false)
    private Course parentCourse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}