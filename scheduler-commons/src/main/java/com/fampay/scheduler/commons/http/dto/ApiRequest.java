package com.fampay.scheduler.commons.http.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiRequest {
    private String url;
    private HttpMethod httpMethod;
    private Object payload;
    private int retries=0;
    private Long readTimeout=120000L;
}
