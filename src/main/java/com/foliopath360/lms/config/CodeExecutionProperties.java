package com.foliopath360.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local code-execution settings.
 *
 * <p>All values can be overridden via environment variables, e.g.
 * {@code CODE_EXECUTION_TIMEOUT_MS} or {@code CODE_EXECUTION_NODE_PATH}.</p>
 */
@ConfigurationProperties(prefix = "code.execution")
public class CodeExecutionProperties {

    /** Maximum wall-clock time a program may run, in milliseconds. */
    private long timeoutMs = 5000;

    /** Maximum amount of stdout/stderr captured per run, in bytes. */
    private long maxOutputBytes = 1048576;

    /** Root folder for temporary execution working directories. */
    private String tempDir = System.getProperty("java.io.tmpdir");

    private String javacPath = "javac";
    private String javaPath = "java";
    private String pythonPath = "python";
    private String nodePath = "node";

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(long maxOutputBytes) {
        this.maxOutputBytes = maxOutputBytes;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public String getJavacPath() {
        return javacPath;
    }

    public void setJavacPath(String javacPath) {
        this.javacPath = javacPath;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public String getPythonPath() {
        return pythonPath;
    }

    public void setPythonPath(String pythonPath) {
        this.pythonPath = pythonPath;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }
}