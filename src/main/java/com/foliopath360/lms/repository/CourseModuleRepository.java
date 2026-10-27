package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.CourseModule;
import com.foliopath360.lms.entity.ModuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {

    List<CourseModule> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    /**
     * The course's sequential learning path, ordered for unlock progression.
     * INACTIVE modules are excluded so they neither block nor count towards
     * {@code totalModules}.
     */
    List<CourseModule> findByCourseIdAndStatusOrderByDisplayOrderAsc(
            UUID courseId, ModuleStatus status);

    Optional<CourseModule> findByModuleCode(String moduleCode);

    boolean existsByModuleCode(String moduleCode);

    boolean existsByCourseIdAndDisplayOrder(UUID courseId, Integer displayOrder);

    void deleteByCourseId(UUID courseId);
}
