package com.foliopath360.lms;

import com.foliopath360.lms.config.CodeExecutionProperties;
import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.execution.CodeExecutor;
import com.foliopath360.lms.execution.ExecutorFactory;
import com.foliopath360.lms.execution.impl.JavaExecutor;
import com.foliopath360.lms.execution.impl.JavaScriptExecutor;
import com.foliopath360.lms.execution.impl.PythonExecutor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the local executors end-to-end without a database.
 * Requires a JDK, Python and Node.js on PATH (as configured in
 * application.properties).
 */
class CodeExecutionExecutorsTest {

    private static ExecutorFactory factory;

    @BeforeAll
    static void setUp() {
        CodeExecutionProperties props = new CodeExecutionProperties();
        props.setTimeoutMs(5000);
        factory = new ExecutorFactory(
                new JavaExecutor(props),
                new PythonExecutor(props),
                new JavaScriptExecutor(props)
        );
    }

    @Test
    void javaRunsAndPrints() {
        CodeExecutor executor = factory.getExecutor("JAVA");
        CodeExecutionResponse response = executor.execute(request(
                "JAVA",
                "public class Demo {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"Hello Java Student\");\n"
                        + "    }\n"
                        + "}",
                ""
        ));
        assertTrue(response.isSuccess(), () -> response.getError());
        assertTrue(response.getOutput().contains("Hello Java Student"));
    }

    @Test
    void javaCompilationErrorIsCaptured() {
        CodeExecutor executor = factory.getExecutor("java");
        CodeExecutionResponse response = executor.execute(request(
                "JAVA",
                "public class Demo {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"Hello\"\n"
                        + "    }\n"
                        + "}",
                ""
        ));
        assertFalse(response.isSuccess());
        assertTrue(response.getError().toLowerCase().contains("compilation error"));
    }

    @Test
    void javaRuntimeErrorIsCaptured() {
        CodeExecutor executor = factory.getExecutor("JAVA");
        CodeExecutionResponse response = executor.execute(request(
                "JAVA",
                "public class Demo {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        int x = 10 / 0;\n"
                        + "    }\n"
                        + "}",
                ""
        ));
        assertFalse(response.isSuccess());
        assertTrue(response.getError().toLowerCase().contains("runtime error"));
    }

    @Test
    void javaTimesOutOnInfiniteLoop() {
        CodeExecutionProperties props = new CodeExecutionProperties();
        props.setTimeoutMs(1500);
        CodeExecutor executor = new JavaExecutor(props);
        CodeExecutionResponse response = executor.execute(request(
                "JAVA",
                "public class Demo {\n"
                        + "    public static void main(String[] args) throws Exception {\n"
                        + "        while (true) { Thread.sleep(100); }\n"
                        + "    }\n"
                        + "}",
                ""
        ));
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("timed out"));
    }

    @Test
    void pythonRunsAndReadsStdin() {
        CodeExecutor executor = factory.getExecutor("PYTHON");
        CodeExecutionResponse response = executor.execute(request(
                "PYTHON",
                "name = input()\nprint('Hello ' + name)",
                "Student\n"
        ));
        assertTrue(response.isSuccess(), () -> response.getError());
        assertTrue(response.getOutput().contains("Hello Student"));
    }

    @Test
    void javaScriptRunsAndPrints() {
        CodeExecutor executor = factory.getExecutor("JAVASCRIPT");
        CodeExecutionResponse response = executor.execute(request(
                "JAVASCRIPT",
                "console.log('Hello JS Student');",
                ""
        ));
        assertTrue(response.isSuccess(), () -> response.getError());
        assertTrue(response.getOutput().contains("Hello JS Student"));
    }

    @Test
    void unsupportedLanguageIsRejected() {
        assertTrue(factory.getExecutor("C++") == null);
        assertTrue(factory.getExecutor("go") == null);
    }

    private CodeExecutionRequest request(String language, String code, String input) {
        return CodeExecutionRequest.builder()
                .language(language)
                .code(code)
                .input(input)
                .build();
    }
}