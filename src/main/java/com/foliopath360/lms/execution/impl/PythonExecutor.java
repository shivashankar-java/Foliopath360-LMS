package com.foliopath360.lms.execution.impl;

import com.foliopath360.lms.config.CodeExecutionProperties;
import com.foliopath360.lms.execution.AbstractProcessExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executes Python via a locally installed interpreter.
 */
@Component
public class PythonExecutor extends AbstractProcessExecutor {

    public PythonExecutor(CodeExecutionProperties properties) {
        super(properties);
    }

    @Override
    protected void prepareSource(Path workDir, String code) throws IOException {
        Files.writeString(
                workDir.resolve("main.py"),
                code == null ? "" : code,
                StandardCharsets.UTF_8
        );
    }

    @Override
    protected String[] buildRunCommand(Path workDir, String code) {
        return new String[]{
                properties.getPythonPath(),
                workDir.resolve("main.py").toString()
        };
    }

    @Override
    public String supportedLanguage() {
        return "PYTHON";
    }
}