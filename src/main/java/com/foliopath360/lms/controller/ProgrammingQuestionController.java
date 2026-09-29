package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ProgrammingQuestionRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionRunRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionSubmitRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionAccessResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmissionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmitResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.ProgrammingQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Standalone programming question bank — not attached to any course.
 * Staff read/write everything. Students read and attempt, but only once they
 * are enrolled in at least one course or interview kit (enforced in the
 * service layer and reported by {@code /student/access}).
 */
@RestController
@RequestMapping("/api/programming-questions")
@RequiredArgsConstructor
public class ProgrammingQuestionController {

    private final ProgrammingQuestionService programmingQuestionService;

    @GetMapping
    public ResponseEntity<List<ProgrammingQuestionResponse>> getProgrammingQuestions(
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.getProgrammingQuestions(requester)
        );
    }

    /**
     * Whether the caller may open the programming question bank. Used by the
     * student sidebar to decide between a normal nav item and a locked one.
     */
    @GetMapping("/student/access")
    public ResponseEntity<ProgrammingQuestionAccessResponse> getAccess(
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(programmingQuestionService.getAccess(requester));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<ProgrammingQuestionResponse> getProgrammingQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.getProgrammingQuestion(questionId, requester)
        );
    }

    @PostMapping("/{questionId}/run")
    public ResponseEntity<CodeExecutionResponse> run(
            @PathVariable UUID questionId,
            @Valid @RequestBody ProgrammingQuestionRunRequest request,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.run(questionId, request, requester)
        );
    }

    @PostMapping("/{questionId}/submit")
    public ResponseEntity<ProgrammingQuestionSubmitResponse> submit(
            @PathVariable UUID questionId,
            @Valid @RequestBody ProgrammingQuestionSubmitRequest request,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.submit(questionId, request, requester)
        );
    }

    /**
     * The authenticated student's submission history for a question.
     * Response is scoped to the requester, so students can only ever read
     * their own submissions.
     */
    @GetMapping("/{questionId}/submissions")
    public ResponseEntity<List<ProgrammingQuestionSubmissionResponse>> getSubmissions(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.getProgrammingQuestionSubmissions(questionId, requester)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ProgrammingQuestionResponse> createProgrammingQuestion(
            @Valid @RequestBody ProgrammingQuestionRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(programmingQuestionService.createProgrammingQuestion(request));
    }

    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<ProgrammingQuestionResponse> updateProgrammingQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody ProgrammingQuestionRequest request
    ) {
        return ResponseEntity.ok(
                programmingQuestionService.updateProgrammingQuestion(questionId, request)
        );
    }

    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteProgrammingQuestion(
            @PathVariable UUID questionId
    ) {
        programmingQuestionService.deleteProgrammingQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
