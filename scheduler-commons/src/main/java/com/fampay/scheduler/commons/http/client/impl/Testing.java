package com.fampay.scheduler.commons.http.client.impl;


import org.asynchttpclient.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class Testing {

    @Autowired
    private WebClient webClient;

    public Mono<String> makeAsyncPostCall(String payload) {
//        return webClient.get().uri("http://localhost:3000/mock-endpoint")
//                .retrieve().bodyToMono(String.class).doOnSuccess(response -> {
//                    // Callback on success
//                    System.out.println("=== CALLBACK: POST Request Completed ===");
//                    System.out.println("Response: " + response);
//                    System.out.println("========================================");
//                });
        return webClient.post()
                .uri("https://jsonplaceholder.typicode.com/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    // Callback on success
                    System.out.println("=== CALLBACK: POST Request Completed ===");
                    System.out.println("Response: " + response);
                    System.out.println("========================================");
                })
                .doOnError(error -> {
                    // Callback on error
                    System.err.println("=== CALLBACK: Request Failed ===");
                    System.err.println("Error: " + error.getMessage());
                })
                .timeout(Duration.ofSeconds(30))
                .retry(2); // Retry failed requests up to 2 times
    }




//
//    private static void makeAsyncPostCall() {
//        // Create async HTTP client
//        AsyncHttpClient client = Dsl.asyncHttpClient();
//
//        // Dummy JSON payload
//        String payload = """
//                {
//                    "name": "John Doe",
//                    "email": "john.doe@example.com",
//                    "age": 30
//                }
//                """;
//
//        System.out.println("Starting async HTTP POST call...");
//
////        // Make async POST request
////        CompletableFuture<Response> future = client
////                .preparePost("https://jsonplaceholder.typicode.com/posts")
////                .setHeader("Content-Type", "application/json")
////                .setBody(payload)
////                .execute()
////                .toCompletableFuture();
//
//        CompletableFuture<Response> future = client.prepareGet("http://localhost:3000/mock-endpoint").execute().toCompletableFuture();
//
//        // Add callback handlers
//        future.thenAccept(response -> {
//            System.out.println("=== CALLBACK: POST Request Completed ===");
//            System.out.println("Status Code: " + response.getStatusCode());
//            System.out.println("Status Text: " + response.getStatusText());
//            System.out.println("Response Body: " + response.getResponseBody());
//            System.out.println("========================================");
//        }).exceptionally(throwable -> {
//            System.err.println("=== CALLBACK: Request Failed ===");
//            System.err.println("Error: " + throwable.getMessage());
//            throwable.printStackTrace();
//            return null;
//        }).whenComplete((result, throwable) -> {
//            // Close the client
//            try {
//                client.close();
//                System.out.println("HTTP Client closed.");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        System.out.println("I can do whatever I want");
//        // Keep application alive until request completes
//        try {
//            future.join();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
