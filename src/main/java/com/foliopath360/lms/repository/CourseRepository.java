package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByCourseCode(String courseCode);

    Optional<Course> findBySlug(String slug);

    List<Course> findByStatus(CourseStatus status);

    boolean existsByCourseCode(String courseCode);

    boolean existsBySlug(String slug);

    long countByStatus(CourseStatus status);
}
