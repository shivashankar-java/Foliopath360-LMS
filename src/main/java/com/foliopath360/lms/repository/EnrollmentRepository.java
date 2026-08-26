package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Enrollment;
import com.foliopath360.lms.entity.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    List<Enrollment> findByUserId(UUID userId);

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseId(UUID userId, UUID courseId);

    boolean existsByUserIdAndCourseIdAndStatusNot(
            UUID userId, UUID courseId, EnrollmentStatus status);

    long countByUserId(UUID userId);

    long countByCourseIdAndStatusNot(UUID courseId, EnrollmentStatus status);

    long countByStatus(com.foliopath360.lms.entity.EnrollmentStatus status);

    // Distinct students with an ongoing enrollment (ENROLLED or IN_PROGRESS)
    @Query("SELECT COUNT(DISTINCT e.user.id) FROM Enrollment e " +
            "WHERE e.status IN :statuses")
    long countActiveStudents(@Param("statuses") Collection<EnrollmentStatus> statuses);

    List<Enrollment> findTop10ByOrderByEnrolledAtDesc();

    List<Enrollment> findByEnrolledAtAfter(java.time.LocalDateTime since);
}
