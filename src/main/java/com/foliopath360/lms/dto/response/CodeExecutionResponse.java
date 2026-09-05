package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeExecutionResponse {

    private boolean success;

    private String output;

    private String error;

    private long executionTime;

    public static CodeExecutionResponse ok(String output, long executionTime) {
        return CodeExecutionResponse.builder()
                .success(true)
                .output(output == null ? "" : output)
                .error(null)
                .executionTime(executionTime)
                .build();
    }

    public static CodeExecutionResponse failure(String error, long executionTime) {
        return CodeExecutionResponse.builder()
                .success(false)
                .output("")
                .error(error == null ? "Execution failed" : error)
                .executionTime(executionTime)
                .build();
    }
}