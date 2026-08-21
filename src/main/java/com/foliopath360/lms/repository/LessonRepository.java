package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    Optional<Lesson> findByLessonCode(String lessonCode);

    boolean existsByLessonCode(String lessonCode);

    boolean existsByModuleIdAndDisplayOrder(UUID moduleId, Integer displayOrder);

    void deleteByModuleId(UUID moduleId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.module.course.id = :courseId")
    long countByCourseId(@Param("courseId") UUID courseId);
}
