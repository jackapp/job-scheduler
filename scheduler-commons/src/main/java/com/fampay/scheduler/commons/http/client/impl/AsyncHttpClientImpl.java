package com.fampay.scheduler.commons.http.client.impl;

import com.fampay.scheduler.commons.http.client.AsyncHttpClient;
import com.fampay.scheduler.commons.http.dto.ApiRequest;
import com.fampay.scheduler.commons.http.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Data
@RequiredArgsConstructor
@Component
public class AsyncHttpClientImpl implements AsyncHttpClient {

    private final WebClient webClient;

    @Override
    public Mono<ApiResponse> callApi(ApiRequest apiRequest) {

        return switch (apiRequest.getHttpMethod().name()) {
            case "POST" -> webClient.post()
                    .uri(apiRequest.getUrl())
                    .bodyValue(apiRequest.getPayload())
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ApiResponse(
                                            body,
                                            clientResponse.statusCode().value())
                                    ))
                    .timeout(Duration.ofSeconds(apiRequest.getReadTimeout()))
                    .retry(apiRequest.getRetries());

            case "GET" -> webClient.get()
                    .uri(apiRequest.getUrl())
                    .exchangeToMono(clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new ApiResponse(
                                            body,
                                            clientResponse.statusCode().value())
                                    ))
                    .timeout(Duration.ofSeconds(apiRequest.getReadTimeout()))
                    .retry(apiRequest.getRetries());
            default -> throw new IllegalArgumentException("Unsupported http method");
        };
    }

}
