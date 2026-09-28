package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.ProgrammingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgrammingQuestionRepository extends JpaRepository<ProgrammingQuestion, UUID> {

    List<ProgrammingQuestion> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    boolean existsByQuestionCode(String questionCode);

    boolean existsByCourseIdAndDisplayOrder(UUID courseId, Integer displayOrder);

    void deleteByCourseId(UUID courseId);
}