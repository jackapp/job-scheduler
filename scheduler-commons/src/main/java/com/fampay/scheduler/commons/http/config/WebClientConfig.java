package com.fampay.scheduler.commons.http.config;

import com.fampay.scheduler.commons.http.dto.ApiResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Map;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(1000)
                .pendingAcquireMaxCount(10000)
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .maxIdleTime(Duration.ofSeconds(100))
                .maxLifeTime(Duration.ofSeconds(100))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .responseTimeout(Duration.ofSeconds(120));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
//
//    public static void main(String[] args) throws InterruptedException {
//        WebClient webClient = webClient();
//        webClient.post()
//                .uri("http://localhost:3000/mock-endpoint")
//                .contentType(MediaType.)
//                .bodyValue(String.valueOf("121212"))
//                .exchangeToMono(clientResponse ->
//                        clientResponse.bodyToMono(String.class)
//                                .defaultIfEmpty("")
//                                .map(body -> new ApiResponse(
//                                        body,
//                                        clientResponse.statusCode().value())
//                                ))
//                .timeout(Duration.ofSeconds(20000))
//                .retry(1)
//                .subscribe(apiResponse -> {
//                    System.out.println(apiResponse.getResponse());
//                    System.out.println(apiResponse.getHttpStatus());
//                });
//        ;
//        Thread.sleep(40000);
//    }
}