package com.foliopath360.lms.execution;

import com.foliopath360.lms.config.CodeExecutionProperties;
import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * Shared local process-based execution logic.
 *
 * <p>For every request a fresh temporary working directory is created,
 * the source file(s) written, the program spawned with a hard timeout,
 * stdin fed from the submission, stdout/stderr captured with a size cap
 * and finally the temporary directory removed. Because each executor
 * extends this class and only supplies the language-specific steps,
 * the safety rails (timeout, output cap, cleanup) apply uniformly.</p>
 */
public abstract class AbstractProcessExecutor implements CodeExecutor {

    protected static final String TIMEOUT_MESSAGE = "Execution timed out";

    protected final CodeExecutionProperties properties;

    protected AbstractProcessExecutor(CodeExecutionProperties properties) {
        this.properties = properties;
    }

    /**
     * Writes the source file(s) for the language into the working directory
     * and performs any build step (e.g. javac). Compilation output must be
     * surfaced through {@link CodeExecutionException} with the compiler
     * message — never a stack trace.
     */
    protected abstract void prepareSource(Path workDir, String code) throws IOException;

    /**
     * Builds the command that runs the ready program from {@code workDir}.
     * The source {@code code} is passed so a stateless executor can derive
     * launch arguments (e.g. the Java main class name) without per-thread
     * state.
     */
    protected abstract String[] buildRunCommand(Path workDir, String code) throws IOException;

    @Override
    public final CodeExecutionResponse execute(CodeExecutionRequest request) {
        Path workDir = createWorkDir();
        long start = System.nanoTime();

        try {
            // Any build step (file write + optional compilation). A compile
            // failure raises CodeExecutionException here, before the run phase.
            prepareSource(workDir, request.getCode());
            return run(workDir, request.getCode(), request.getInput(), start);
        } catch (CodeExecutionException e) {
            // Compilation / language errors — returned as a failed response.
            return CodeExecutionResponse.failure(e.getMessage(), Math.max(elapsedMs(start), 1));
        } catch (IOException e) {
            return CodeExecutionResponse.failure(
                    "Execution environment error", Math.max(elapsedMs(start), 1)
            );
        } finally {
            deleteQuietly(workDir);
        }
    }

    private CodeExecutionResponse run(Path workDir, String code, String input, long start) {
        Process process = null;
        try {
            process = new ProcessBuilder(buildRunCommand(workDir, code))
                    .directory(workDir.toFile())
                    .redirectErrorStream(false)
                    .start();

            BoundedSink stdout = new BoundedSink();
            BoundedSink stderr = new BoundedSink();
            Thread outReader = drain(process.getInputStream(), stdout);
            Thread errReader = drain(process.getErrorStream(), stderr);

            feedStdin(process.getOutputStream(), input);

            boolean finished = process.waitFor(
                    properties.getTimeoutMs(), TimeUnit.MILLISECONDS
            );
            long elapsed = elapsedMs(start);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                return CodeExecutionResponse.failure(TIMEOUT_MESSAGE, elapsed);
            }

            outReader.join(2_000);
            errReader.join(2_000);

            String out = stdout.asString();
            String err = stderr.asString();
            long finalElapsed = elapsed;
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                return CodeExecutionResponse.ok(out, finalElapsed);
            }

            String diagnostic = !err.isEmpty() ? err : out;
            String message = "Runtime error (exit code " + exitCode + ")"
                    + (diagnostic.isEmpty() ? "" : ":\n" + diagnostic);
            return CodeExecutionResponse.failure(message, finalElapsed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return CodeExecutionResponse.failure("Execution was interrupted", elapsedMs(start));
        } catch (IOException e) {
            return CodeExecutionResponse.failure(
                    "Execution environment error", elapsedMs(start)
            );
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Path createWorkDir() {
        try {
            Path root = Path.of(properties.getTempDir());
            Files.createDirectories(root);
            return Files.createTempDirectory(root, "foliopath-code-");
        } catch (IOException e) {
            throw new CodeExecutionException("Unable to create execution workspace", e);
        }
    }

    protected Thread drain(InputStream stream, BoundedSink sink) {
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            int read;
            try (InputStream in = stream) {
                while ((read = in.read(buffer)) != -1) {
                    sink.append(buffer, 0, read);
                }
            } catch (IOException ignored) {
                // Stream closed by the process/cleanup — final content is kept.
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void feedStdin(OutputStream stream, String input) {
        try (OutputStream out = stream) {
            if (input != null && !input.isEmpty()) {
                out.write(input.getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
        } catch (IOException ignored) {
            // Program did not read stdin — fine.
        }
    }

    private long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    /**
     * Bounded byte sink: appends process output but never exceeds the
     * configured maximum, so a chatty/binary program cannot blow up memory.
     */
    protected class BoundedSink {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public BoundedSink() {
        }

        public void append(byte[] bytes, int offset, int length) {
            long cap = properties.getMaxOutputBytes();
            if (buffer.size() >= cap) {
                return;
            }
            int allowed = (int) Math.min(length, cap - buffer.size());
            buffer.write(bytes, offset, allowed);
        }

        public String asString() {
            String value = buffer.toString(StandardCharsets.UTF_8);
            if (buffer.size() >= properties.getMaxOutputBytes()) {
                return value + "\n\n... (output truncated)";
            }
            return value;
        }
    }
}