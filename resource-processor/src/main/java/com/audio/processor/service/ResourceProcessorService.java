package com.audio.processor.service;

import com.audio.processor.dto.SongMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class ResourceProcessorService {

    private final WebClient resourceServiceClient;
    private final WebClient songServiceClient;
    private final Mp3MetadataExtractor extractor;
    private final StreamBridge streamBridge;

    public ResourceProcessorService(@Qualifier("resourceServiceClient") WebClient resourceServiceClient,
                                    @Qualifier("songServiceClient") WebClient songServiceClient,
                                    Mp3MetadataExtractor extractor,
                                    StreamBridge streamBridge) {
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
        this.extractor = extractor;
        this.streamBridge = streamBridge;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public byte[] fetchResource(Long id) {
        return resourceServiceClient.get()
                .uri("/resources/{id}", id)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void saveSongMetadata(SongMetadata metadata) {
        songServiceClient.post()
                .uri("/songs")
                .bodyValue(metadata)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public SongMetadata process(Long resourceId) {
        log.info("Processing resource: {}", resourceId);
        byte[] data = fetchResource(resourceId);
        SongMetadata metadata = extractor.extract(data);
        metadata.setId(resourceId);
        saveSongMetadata(metadata);
        log.info("Publishing processedResource completion event for ID: {}", resourceId);
        streamBridge.send("resourceProcessed-out-0", resourceId);

        return metadata;
    }
}
