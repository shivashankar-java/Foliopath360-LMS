package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.MockTestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
