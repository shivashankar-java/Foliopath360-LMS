package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InterviewKit;
import com.foliopath360.lms.entity.KitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewKitRepository extends JpaRepository<InterviewKit, UUID> {

    Optional<InterviewKit> findByKitCode(String kitCode);

    Optional<InterviewKit> findBySlug(String slug);

    List<InterviewKit> findByStatus(KitStatus status);

    long countByStatus(KitStatus status);

    boolean existsByKitCode(String kitCode);

    boolean existsBySlug(String slug);
}
