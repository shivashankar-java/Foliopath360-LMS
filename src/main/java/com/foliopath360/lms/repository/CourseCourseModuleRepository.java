package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.CourseCourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseCourseModuleRepository extends JpaRepository<CourseCourseModule, UUID> {

    List<CourseCourseModule> findByParentCourseIdOrderByDisplayOrderAsc(UUID parentCourseId);

    List<CourseCourseModule> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    Optional<CourseCourseModule> findByParentCourseIdAndCourseId(UUID parentCourseId, UUID courseId);

    boolean existsByParentCourseIdAndCourseId(UUID parentCourseId, UUID courseId);

    int countByParentCourseId(UUID parentCourseId);

    void deleteByParentCourseId(UUID parentCourseId);

    void deleteById(UUID id);

    Optional<CourseCourseModule> findByIdAndParentCourseId(UUID id, UUID parentCourseId);
}