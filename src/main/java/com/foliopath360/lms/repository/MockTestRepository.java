package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MockTestRepository extends JpaRepository<MockTest, UUID> {

    List<MockTest> findByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    boolean existsByTestCode(String testCode);

    boolean existsByModuleIdAndDisplayOrder(UUID moduleId, Integer displayOrder);

    void deleteByModuleId(UUID moduleId);
}
