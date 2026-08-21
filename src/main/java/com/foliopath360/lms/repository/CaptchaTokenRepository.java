package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.CaptchaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CaptchaTokenRepository extends JpaRepository<CaptchaToken, String> {

    long deleteByExpiresAtBefore(LocalDateTime dateTime);
}
