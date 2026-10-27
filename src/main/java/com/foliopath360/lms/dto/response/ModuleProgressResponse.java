package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Sequential module progress for one student in one course.
 *
 * <pre>
 * Progress % = (completed modules / total modules) x 100
 * </pre>
 *
 * where a module counts as completed only once its mock test has been
 * passed - never because the module was opened or viewed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleProgressResponse {

    /** {@code student_code} (e.g. {@code S000003}) when available. */
    private String studentId;

    private UUID studentUserId;
    private UUID courseId;
    private String courseName;
    private UUID enrollmentId;
    private String enrollmentStatus;

    private Integer totalModules;
    private Integer completedModules;
    private Integer progressPercentage;

    /** 1-based number of the module the student is currently on. */
    private Integer currentModule;

    /** True once every module is completed (progressPercentage == 100). */
    private Boolean completed;

    private List<ModuleProgressItemResponse> modules;
}
