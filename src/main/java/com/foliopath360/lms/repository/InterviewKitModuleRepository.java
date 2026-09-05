package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InterviewKitModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewKitModuleRepository extends JpaRepository<InterviewKitModule, UUID> {

    @Query(value = "SELECT COUNT(*) FROM kit_modules WHERE module_id = :moduleId", nativeQuery = true)
    long countKitsByModuleId(@Param("moduleId") UUID moduleId);
}