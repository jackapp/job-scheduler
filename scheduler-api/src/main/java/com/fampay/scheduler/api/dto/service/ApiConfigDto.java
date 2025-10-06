package com.fampay.scheduler.api.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiConfigDto {
    private String url;
    private HttpMethod httpMethod;
    private Object payload;
    private Long readTimeoutMs=120000L;
}
