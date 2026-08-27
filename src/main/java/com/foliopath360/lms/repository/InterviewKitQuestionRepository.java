package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InterviewKitQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewKitQuestionRepository extends JpaRepository<InterviewKitQuestion, UUID> {

    List<InterviewKitQuestion> findByKitIdOrderByDisplayOrderAsc(UUID kitId);

    long countByKitId(UUID kitId);

    void deleteByKitId(UUID kitId);
}
