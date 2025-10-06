package com.fampay.scheduler.commons.http.client;

import com.fampay.scheduler.commons.http.dto.ApiRequest;
import com.fampay.scheduler.commons.http.dto.ApiResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public interface AsyncHttpClient {
    Mono<ApiResponse> callApi(ApiRequest apiRequest);
}
