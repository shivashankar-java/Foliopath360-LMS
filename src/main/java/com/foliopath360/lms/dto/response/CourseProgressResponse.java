package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseProgressResponse {

    private UUID courseId;

    private UUID enrollmentId;

    private String enrollmentStatus;

    private long totalLessons;

    private long completedLessons;

    private Integer progressPercentage;

    private Set<UUID> completedLessonIds;
}
