package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InterviewKitEnrollment;
import com.foliopath360.lms.entity.KitEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewKitEnrollmentRepository extends JpaRepository<InterviewKitEnrollment, UUID> {

    List<InterviewKitEnrollment> findByUserId(UUID userId);

    Optional<InterviewKitEnrollment> findByUserIdAndKitId(UUID userId, UUID kitId);

    boolean existsByUserIdAndKitId(UUID userId, UUID kitId);

    boolean existsByUserIdAndKitIdAndStatusNot(UUID userId, UUID kitId, KitEnrollmentStatus status);

    long countByKitIdAndStatusNot(UUID kitId, KitEnrollmentStatus status);
}
