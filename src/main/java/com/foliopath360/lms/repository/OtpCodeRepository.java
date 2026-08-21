package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.OtpCode;
import com.foliopath360.lms.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedDtDesc(
            String email,
            OtpPurpose purpose
    );

    long deleteByExpiresAtBefore(LocalDateTime dateTime);
}
