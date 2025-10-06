package com.fampay.scheduler.api.dto.request;

import lombok.Data;

@Data
public class ApiConfig {
    private String url;
    private String httpMethod;
    private Object payload;
    private Long readTimeoutMs=90000L;
}
