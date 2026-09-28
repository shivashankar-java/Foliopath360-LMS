package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.ProgrammingQuestionSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgrammingQuestionSubmissionRepository
        extends JpaRepository<ProgrammingQuestionSubmission, UUID> {

    List<ProgrammingQuestionSubmission>
            findByQuestionIdAndUserIdOrderBySubmittedAtDesc(UUID questionId, UUID userId);

    List<ProgrammingQuestionSubmission>
            findByUserIdAndQuestion_CourseIdOrderBySubmittedAtDesc(UUID userId, UUID courseId);
}