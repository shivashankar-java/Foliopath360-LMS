package com.foliopath360.lms.execution;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;

/**
 * Executes a single submitted code snippet in a specific programming language.
 *
 * <p>Implementations are local process-based executors today. The
 * {@link CodeExecutionService} exposes a single entry point so the whole
 * execution layer can later be swapped for Docker/container sandboxing
 * without changing the API or the frontend.</p>
 */
public interface CodeExecutor {

    /**
     * Executes the given source code and returns the result.
     *
     * @param request the submission (language + current student code + stdin)
     * @return a sanitised result; never leaks server paths or stack traces
     */
    CodeExecutionResponse execute(CodeExecutionRequest request);

    /**
     * Normalised language key this executor handles, e.g. {@code JAVA}.
     */
    String supportedLanguage();
}