package com.fampay.scheduler.models.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiConfig {
    private String url;
    private String httpMethod;
    private Object payload;
    private Long readTimeoutMs;
}
