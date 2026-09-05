package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InterviewKitQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewKitQuestionRepository extends JpaRepository<InterviewKitQuestion, UUID> {

    List<InterviewKitQuestion> findByKitIdOrderByDisplayOrderAsc(UUID kitId);

    List<InterviewKitQuestion> findByModuleIdOrderByDisplayOrderAsc(UUID moduleId);

    List<InterviewKitQuestion> findByModuleIdInOrderByDisplayOrderAsc(List<UUID> moduleIds);

    List<InterviewKitQuestion> findByKitIdAndModuleIsNull(UUID kitId);

    long countByKitId(UUID kitId);

    long countByModuleId(UUID moduleId);

    long countByModuleIdIn(List<UUID> moduleIds);

    long countByKitIdAndModuleIsNull(UUID kitId);

    void deleteByKitId(UUID kitId);

    void deleteByKitIdAndModuleIdIsNull(UUID kitId);

    @Modifying
    @Query(value = "UPDATE interview_kit_questions SET kit_id = NULL WHERE kit_id = :kitId AND module_id IS NOT NULL", nativeQuery = true)
    int clearKitIdForModuleQuestions(@Param("kitId") UUID kitId);
}