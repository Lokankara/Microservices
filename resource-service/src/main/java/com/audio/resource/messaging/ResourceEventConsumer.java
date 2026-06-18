package com.audio.resource.messaging;

import com.audio.resource.exception.ResourceNotFoundException;
import com.audio.resource.entity.ResourceEntity;
import com.audio.resource.entity.StorageType;
import com.audio.resource.repository.ResourceRepository;
import com.audio.resource.service.S3StorageService;
import com.audio.resource.service.StorageServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class ResourceEventConsumer {

    private final ResourceRepository repository;
    private final StorageServiceClient storageServiceClient;
    private final S3StorageService s3StorageService;

    public ResourceEventConsumer(ResourceRepository repository,
                                 StorageServiceClient storageServiceClient,
                                 S3StorageService s3StorageService) {
        this.repository = repository;
        this.storageServiceClient = storageServiceClient;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public void handleResourceProcessed(Long resourceId) {
        ResourceEntity entity = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + resourceId + " not found"));

        if (entity.getStorageType() == StorageType.PERMANENT) {
            log.info("Resource {} already in PERMANENT state, skipping", resourceId);
            return;
        }

        var storageResponse = storageServiceClient.getAllStorages();
        var permanentStorage = storageResponse.getStorages().stream()
                .filter(s -> s.getStorageType() == StorageType.PERMANENT)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PERMANENT storage not found"));

        String destBucket = permanentStorage.getBucket();
        String destPath = permanentStorage.getPath();

        s3StorageService.move(entity.getStorageBucket(), entity.getStoragePath(),
                destBucket, destPath, entity.getStorageKey());

        entity.setStorageType(StorageType.PERMANENT);
        entity.setStorageBucket(destBucket);
        entity.setStoragePath(destPath);
        repository.save(entity);

        log.info("Resource {} moved from STAGING to PERMANENT (bucket={}, path={})",
                resourceId, destBucket, destPath);
    }
}
