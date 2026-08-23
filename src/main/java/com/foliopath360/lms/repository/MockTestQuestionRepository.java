package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.MockTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MockTestQuestionRepository extends JpaRepository<MockTestQuestion, UUID> {

    boolean existsByQuestionCode(String questionCode);
}
