package com.foliopath360.lms.execution;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.execution.impl.JavaExecutor;
import com.foliopath360.lms.execution.impl.JavaScriptExecutor;
import com.foliopath360.lms.execution.impl.PythonExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Selects the correct {@link CodeExecutor} for a submitted language.
 *
 * <p>Adding a new language (C, C++, KOTLIN, GO, ...) is done by:
 * <ol>
 *   <li>implementing the {@link CodeExecutor} interface, and</li>
 *   <li>registering it in {@link #register(CodeExecutor)}.</li>
 * </ol>
 */
@Component
public class ExecutorFactory {

    private final Map<String, CodeExecutor> executors = new LinkedHashMap<>();

    public ExecutorFactory(
            JavaExecutor javaExecutor,
            PythonExecutor pythonExecutor,
            JavaScriptExecutor javaScriptExecutor
    ) {
        register(javaExecutor);
        register(pythonExecutor);
        register(javaScriptExecutor);
    }

    private void register(CodeExecutor executor) {
        executors.put(executor.supportedLanguage(), executor);
    }

    /**
     * @return the executor for the given language, case-insensitive,
     *         or {@code null} when the language is not supported.
     */
    public CodeExecutor getExecutor(String language) {
        if (language == null) {
            return null;
        }
        return executors.get(
                language.trim().toUpperCase(Locale.ROOT)
        );
    }

    public List<String> supportedLanguages() {
        return List.copyOf(executors.keySet());
    }
}