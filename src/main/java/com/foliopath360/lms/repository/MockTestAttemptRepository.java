package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.MockTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockTestAttemptRepository extends JpaRepository<MockTestAttempt, UUID> {

    List<MockTestAttempt> findByUserIdAndMockTestIdOrderBySubmittedAtDesc(
            UUID userId, UUID mockTestId);

    Optional<MockTestAttempt> findFirstByUserIdAndMockTestIdOrderBySubmittedAtDesc(
            UUID userId, UUID mockTestId);

    boolean existsByUserIdAndMockTestId(UUID userId, UUID mockTestId);

    List<MockTestAttempt> findByUserIdOrderBySubmittedAtDesc(UUID userId);

    List<MockTestAttempt> findByMockTestIdOrderBySubmittedAtDesc(UUID mockTestId);

    long countByMockTestId(UUID mockTestId);

    // ------------------------------------------------------------------
    // Module completion: the single source of truth for course progress.
    // Only PASSED attempts count - a failed attempt never advances a module.
    // ------------------------------------------------------------------

    /**
     * True when the student has a passed attempt on any mock test of the
     * given module, i.e. the module is complete and the next one unlocks.
     */
    boolean existsByUserIdAndPassedTrueAndMockTestModuleId(
            UUID userId, UUID moduleId);

    /**
     * Every passed attempt of the student across the given course's modules,
     * oldest first, so the caller can derive both the set of completed
     * modules and each one's completion timestamp.
     */
    @Query("""
            SELECT a
            FROM MockTestAttempt a
            WHERE a.user.id = :userId
              AND a.passed = true
              AND a.mockTest.module.course.id = :courseId
            ORDER BY a.submittedAt ASC
            """)
    List<MockTestAttempt> findPassedAttemptsByUserIdAndCourseId(
            @Param("userId") UUID userId, @Param("courseId") UUID courseId);
}
