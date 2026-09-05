package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.execution.CodeExecutor;
import com.foliopath360.lms.execution.ExecutorFactory;
import com.foliopath360.lms.service.CodeExecutionService;
import org.springframework.stereotype.Service;

/**
 * Delegates an execution request to the executor selected by the factory.
 */
@Service
public class CodeExecutionServiceImpl implements CodeExecutionService {

    private final ExecutorFactory executorFactory;

    public CodeExecutionServiceImpl(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    @Override
    public CodeExecutionResponse execute(CodeExecutionRequest request) {
        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            return CodeExecutionResponse.failure("Code to execute must not be empty", 0);
        }

        CodeExecutor executor = executorFactory.getExecutor(request.getLanguage());
        if (executor == null) {
            return CodeExecutionResponse.failure(
                    "Unsupported language: " + request.getLanguage()
                            + ". Supported languages: "
                            + String.join(", ", executorFactory.supportedLanguages()),
                    0
            );
        }

        return executor.execute(request);
    }
}