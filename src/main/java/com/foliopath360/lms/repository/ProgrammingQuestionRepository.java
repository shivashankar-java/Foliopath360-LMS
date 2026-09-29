package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.ProgrammingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgrammingQuestionRepository extends JpaRepository<ProgrammingQuestion, UUID> {

    List<ProgrammingQuestion> findAllByOrderByDisplayOrderAsc();

    List<ProgrammingQuestion> findByDifficultyOrderByDisplayOrderAsc(String difficulty);

    boolean existsByQuestionCode(String questionCode);
}
