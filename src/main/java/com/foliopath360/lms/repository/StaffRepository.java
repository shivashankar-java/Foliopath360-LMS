package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    Optional<Staff> findByUserId(UUID userId);

    Optional<Staff> findByStaffCode(String staffCode);

    boolean existsByStaffCode(String staffCode);

    boolean existsByUserId(UUID userId);
}
