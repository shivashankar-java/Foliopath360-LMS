package com.foliopath360.lms.execution.impl;

import com.foliopath360.lms.config.CodeExecutionProperties;
import com.foliopath360.lms.execution.AbstractProcessExecutor;
import com.foliopath360.lms.execution.CodeExecutionException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles and runs Java source with a locally installed JDK (javac + java).
 *
 * <p>The submitted editor may contain any top-level public type
 * ({@code public class Demo} / {@code public class HelloWorld} / ...), so the
 * file name is derived from the declared type rather than assuming
 * {@code Main}. The package declaration is stripped so the flat temporary
 * directory compiles cleanly.</p>
 */
@Component
public class JavaExecutor extends AbstractProcessExecutor {

    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:final\\s+|abstract\\s+)*(?:class|interface|enum|record)\\s+"
                    + "([A-Za-z_$][\\w$]*)"
    );

    private static final Pattern ANY_TYPE = Pattern.compile(
            "\\b(?:class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)"
    );

    private static final Pattern PACKAGE_DECL = Pattern.compile(
            "(?m)^\\s*package\\s+[^;]+;"
    );

    public JavaExecutor(CodeExecutionProperties properties) {
        super(properties);
    }

    @Override
    protected void prepareSource(Path workDir, String code) throws IOException {
        String source = stripPackage(code);
        String className = detectClassName(source);
        Path sourceFile = workDir.resolve(className + ".java");
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        Path outputDir = workDir.resolve("out");
        Files.createDirectories(outputDir);

        compile(sourceFile, outputDir);
    }

    @Override
    protected String[] buildRunCommand(Path workDir, String code) {
        String className = detectClassName(stripPackage(code));
        Path outputDir = workDir.resolve("out");
        return new String[]{
                properties.getJavaPath(),
                "-cp",
                outputDir.toString(),
                className
        };
    }

    @Override
    public String supportedLanguage() {
        return "JAVA";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void compile(Path sourceFile, Path outputDir) {
        Process process = null;
        try {
            process = new ProcessBuilder(
                    properties.getJavacPath(),
                    "-encoding", "UTF-8",
                    "-d", outputDir.toString(),
                    sourceFile.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            BoundedSink sink = new BoundedSink();
            Thread reader = drain(process.getInputStream(), sink);

            boolean finished = process.waitFor(
                    properties.getTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS
            );
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS);
                throw new CodeExecutionException("Compilation timed out");
            }

            reader.join(2_000);

            if (process.exitValue() != 0) {
                String diagnostic = sink.asString().trim();
                throw new CodeExecutionException(
                        "Compilation error"
                                + (diagnostic.isEmpty() ? "" : ":\n" + diagnostic)
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new CodeExecutionException("Compilation was interrupted");
        } catch (IOException e) {
            throw new CodeExecutionException(
                    "Compilation environment error: javac is not available or failed to start"
            );
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private String stripPackage(String code) {
        if (code == null) {
            return "";
        }
        return PACKAGE_DECL.matcher(code).replaceAll("");
    }

    private String detectClassName(String source) {
        Matcher publicType = PUBLIC_TYPE.matcher(source);
        if (publicType.find()) {
            return publicType.group(1);
        }
        Matcher anyType = ANY_TYPE.matcher(source);
        if (anyType.find()) {
            return anyType.group(1);
        }
        return "Main";
    }
}