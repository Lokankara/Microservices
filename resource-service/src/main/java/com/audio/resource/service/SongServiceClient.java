package com.audio.resource.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RefreshScope
public class SongServiceClient {

    private final WebClient webClient;

    public SongServiceClient(
            @Value("${song.service.url:http://localhost:8082}") String songServiceUrl,
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(songServiceUrl).build();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @CircuitBreaker(name = "songService", fallbackMethod = "deleteSongMetadataFallback")
    public void deleteSongMetadata(String ids) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/songs").queryParam("id", ids).build())
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void deleteSongMetadataFallback(String ids, Throwable t) {
        log.warn("Circuit breaker OPEN for song-service. Could not delete metadata for IDs: {}. Reason: {}", ids, t.getMessage());
    }
}
