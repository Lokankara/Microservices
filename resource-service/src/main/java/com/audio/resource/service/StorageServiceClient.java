package com.audio.resource.service;

import com.audio.resource.dto.StorageResponse;
import com.audio.resource.entity.StorageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Slf4j
@Component
@RefreshScope
public class StorageServiceClient {

    private final WebClient webClient;
    private final StorageResponse.StubConfig stubConfig;

    public StorageServiceClient(
            @Value("${storage.service.url:http://localhost:8085}") String storageServiceUrl,
            StorageResponse.StubConfigProvider stubConfigProvider) {
        this.webClient = WebClient.builder().baseUrl(storageServiceUrl).build();
        this.stubConfig = stubConfigProvider.getStubConfig();
        log.info("StorageServiceClient initialized with stub fallback: staging={}/{}, permanent={}/{}",
                stubConfig.stagingBucket(), stubConfig.stagingPath(),
                stubConfig.permanentBucket(), stubConfig.permanentPath());
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    @CircuitBreaker(name = "storageService", fallbackMethod = "getStoragesByTypeFallback")
    public StorageResponse getStoragesByType(StorageType storageType) {
        log.info("Calling Storage Service for storage type: {}", storageType);
        StorageResponse response = fetchStorages();
        long matching = response.getStorages() == null
                ? 0
                : response.getStorages().stream().filter(s -> s.getStorageType() == storageType).count();
        log.info("Storage Service returned {} storages ({} match type={})",
                response.getStorages() == null ? 0 : response.getStorages().size(),
                matching, storageType);
        return response;
    }

    public StorageResponse getStoragesByTypeFallback(StorageType storageType, Throwable t) {
        log.warn("Circuit breaker OPEN for storage-service. Using stub data for type={}. Reason: {}",
                storageType, t.getMessage());
        return StorageResponse.stub(stubConfig);
    }

    @Retryable(backoff = @Backoff(delay = 1000, multiplier = 2))
    @CircuitBreaker(name = "storageService", fallbackMethod = "getAllStoragesFallback")
    public StorageResponse getAllStorages() {
        log.info("Calling Storage Service to get all storages");
        return fetchStorages();
    }

    public StorageResponse getAllStoragesFallback(Throwable t) {
        log.warn("Circuit breaker OPEN for storage-service. Returning stub data. Reason: {}", t.getMessage());
        return StorageResponse.stub(stubConfig);
    }

    private StorageResponse fetchStorages() {
        List<StorageResponse.StorageDto> storages = webClient.get()
                .uri("/storages")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<StorageResponse.StorageDto>>() {
                })
                .block();

        StorageResponse response = new StorageResponse();
        response.setStorages(storages);
        return response;
    }
}
