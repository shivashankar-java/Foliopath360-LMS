package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;

/**
 * Single entry point for executing student code.
 *
 * <p>The interface deliberately isolates the API from the concrete executor
 * strategy so the underlying sandbox (local processes today, Docker or a
 * remote judge later) can be swapped without touching the controller or the
 * frontend.</p>
 */
public interface CodeExecutionService {

    CodeExecutionResponse execute(CodeExecutionRequest request);
}