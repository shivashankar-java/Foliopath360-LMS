package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.service.CodeExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executes the student's current code.
 *
 * <p>This endpoint is intentionally separate from the lesson/course-update
 * APIs: running code never writes to the admin's starter code.</p>
 */
@RestController
@RequestMapping("/api/code")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    public CodeExecutionController(CodeExecutionService codeExecutionService) {
        this.codeExecutionService = codeExecutionService;
    }

    @PostMapping("/execute")
    public ResponseEntity<CodeExecutionResponse> execute(
            @Valid @RequestBody CodeExecutionRequest request
    ) {
        return ResponseEntity.ok(codeExecutionService.execute(request));
    }
}