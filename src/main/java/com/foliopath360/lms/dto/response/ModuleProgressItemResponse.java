package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single module's row in {@link ModuleProgressResponse}.
 *
 * <p>{@code status} and {@code unlocked} are authoritative and server-side;
 * the client must not derive them itself.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleProgressItemResponse {

    private UUID moduleId;
    private String moduleCode;
    private String moduleName;
    private String description;

    /** 1-based position of the module inside the course sequence. */
    private Integer moduleNumber;

    /** The module's own display order (may start at 0). */
    private Integer displayOrder;

    /** One of {@code LOCKED}, {@code OPEN}, {@code COMPLETED}. */
    private String status;

    /** Convenience flag: {@code true} for {@code OPEN} and {@code COMPLETED}. */
    private Boolean unlocked;

    /**
     * True only when the student has a <em>passed</em> attempt on this
     * module's mock test. This - and only this - drives course progress.
     */
    private Boolean mockTestCompleted;

    /** Whether the module actually has a mock test attached. */
    private Boolean hasMockTest;

    /** The mock test that gates this module (the lowest-ordered one). */
    private UUID mockTestId;
    private String mockTestTitle;

    /** When the passing attempt was submitted, {@code null} if not passed. */
    private LocalDateTime completedAt;

    private Integer totalLessons;
    private Integer completedLessons;
    private Integer lessonProgressPercentage;
}
