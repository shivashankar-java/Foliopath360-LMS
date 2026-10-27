package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockTestRepository extends JpaRepository<MockTest, UUID> {

    List<MockTest> findByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    /**
     * All mock tests across a course's modules in one round trip, so the
     * progress payload can resolve which module has a gating test (and
     * therefore which modules are auto-completed) without an N+1 loop.
     */
    @Query("""
            SELECT t
            FROM MockTest t
            WHERE t.module.course.id = :courseId
            ORDER BY t.module.displayOrder ASC, t.displayOrder ASC
            """)
    List<MockTest> findByCourseIdOrderByModuleOrder(@Param("courseId") UUID courseId);

    /**
     * The mock test that gates a module - the lowest-ordered one. A module
     * can hold several tests; passing any one of them completes the module,
     * but this is the one surfaced in the progress payload.
     */
    Optional<MockTest> findFirstByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    boolean existsByTestCode(String testCode);

    boolean existsByModuleIdAndDisplayOrder(UUID moduleId, Integer displayOrder);

    void deleteByModuleId(UUID moduleId);
}
