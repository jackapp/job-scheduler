package com.fampay.scheduler.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {
    @NotBlank(message = "Schedule must not be blank")
    private String schedule;
    @NotNull
    private ApiConfig api;

    @NotBlank(message = "Type must not be blank")
    @Pattern(regexp = "ATMOST_ONCE|ATLEAST_ONCE",
            message = "Type must be either ATMOST_ONCE or ATLEAST_ONCE")
    private String type;
    private String correlationId;
}