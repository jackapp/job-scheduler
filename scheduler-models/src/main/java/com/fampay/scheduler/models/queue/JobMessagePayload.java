package com.fampay.scheduler.models.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobMessagePayload {
    private String executionId;
    private long scheduledRunAt;
    private String jobId;
    private String type;
    private ApiConfig apiConfig;
    private long createdAt;
    private long updatedAt;
}
