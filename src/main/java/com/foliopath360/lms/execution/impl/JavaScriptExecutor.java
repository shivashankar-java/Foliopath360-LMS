package com.foliopath360.lms.execution.impl;

import com.foliopath360.lms.config.CodeExecutionProperties;
import com.foliopath360.lms.execution.AbstractProcessExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Executes JavaScript via a locally installed Node.js runtime.
 */
@Component
public class JavaScriptExecutor extends AbstractProcessExecutor {

    public JavaScriptExecutor(CodeExecutionProperties properties) {
        super(properties);
    }

    @Override
    protected void prepareSource(Path workDir, String code) throws IOException {
        Files.writeString(
                workDir.resolve("main.js"),
                code == null ? "" : code,
                StandardCharsets.UTF_8
        );
    }

    @Override
    protected String[] buildRunCommand(Path workDir, String code) {
        return new String[]{
                properties.getNodePath(),
                workDir.resolve("main.js").toString()
        };
    }

    @Override
    public String supportedLanguage() {
        return "JAVASCRIPT";
    }
}