package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    List<LessonProgress> findByUserIdAndLessonModuleCourseId(UUID userId, UUID courseId);

    long countByUserIdAndLessonModuleCourseId(UUID userId, UUID courseId);
}
