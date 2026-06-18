package com.audio.resource.service;

import com.audio.resource.dto.ResourceDeleteResponse;
import com.audio.resource.dto.ResourceUploadResponse;
import com.audio.resource.dto.StorageResponse;
import com.audio.resource.entity.ResourceEntity;
import com.audio.resource.entity.StorageType;
import com.audio.resource.exception.InvalidRequestException;
import com.audio.resource.exception.ResourceNotFoundException;
import com.audio.resource.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ResourceService {

    private final ResourceRepository repository;
    private final SongServiceClient songServiceClient;
    private final StorageServiceClient storageServiceClient;
    private final S3StorageService s3StorageService;
    private final ResourceEventPublisher eventPublisher;

    public ResourceService(ResourceRepository repository,
            SongServiceClient songServiceClient,
            StorageServiceClient storageServiceClient,
            S3StorageService s3StorageService,
            ResourceEventPublisher eventPublisher) {
        this.repository = repository;
        this.songServiceClient = songServiceClient;
        this.storageServiceClient = storageServiceClient;
        this.s3StorageService = s3StorageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ResourceUploadResponse upload(HttpServletRequest request) {
        try {
            byte[] data = request.getInputStream().readAllBytes();

            StorageResponse storageResponse = storageServiceClient.getStoragesByType(StorageType.STAGING);
            if (storageResponse.isStubData()) {
                log.warn("Using stub storage data - Storage Service may be unavailable");
            }

            StorageResponse.StorageDto stagingStorage = storageResponse.getStorages().stream()
                    .filter(s -> s.getStorageType() == StorageType.STAGING)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "STAGING storage not returned by Storage Service (stub=" + storageResponse.isStubData() +
                                    ")"));

            String bucket = stagingStorage.getBucket();
            String path = stagingStorage.getPath();

            String storageKey = UUID.randomUUID().toString();
            s3StorageService.upload(bucket, path, storageKey, data);

            ResourceEntity entity = new ResourceEntity(storageKey, StorageType.STAGING, bucket, path);
            ResourceEntity saved = repository.save(entity);

            eventPublisher.publishUploadEvent(saved.getId());

            return new ResourceUploadResponse(saved.getId(), s3StorageService.getUrl(bucket, path, storageKey));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getById(String id) {
        ResourceEntity entity = repository.findById(validateId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));

        return s3StorageService.download(entity.getStorageBucket(), entity.getStoragePath(), entity.getStorageKey());
    }

    @Transactional
    public ResourceDeleteResponse delete(String ids) {
        if (ids == null) {
            throw new InvalidRequestException("CSV string must not be null");
        }
        if (ids.length() > 200) {
            throw new InvalidRequestException(
                    "CSV string is too long: received " + ids.length() + " characters, maximum allowed is 200");
        }

        String[] parts = ids.split(",");
        List<Long> deletedIds = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            try {
                long id = Long.parseLong(trimmed);
                if (id <= 0) {
                    throw new NumberFormatException();
                }

                Optional<ResourceEntity> entityOpt = repository.findById(id);
                if (entityOpt.isPresent()) {
                    ResourceEntity entity = entityOpt.get();
                    s3StorageService.delete(entity.getStorageBucket(), entity.getStoragePath(), entity.getStorageKey());
                    repository.deleteById(id);
                    songServiceClient.deleteSongMetadata(String.valueOf(id));
                    deletedIds.add(id);
                }
            } catch (NumberFormatException e) {
                throw new InvalidRequestException(
                        "Invalid ID format: '" + trimmed + "'. Only positive integers are allowed");
            }
        }

        return new ResourceDeleteResponse(deletedIds);
    }

    public long validateId(String rawId) {
        try {
            long id = Long.parseLong(rawId);
            if (id > 0) {
                return id;
            }
            throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new InvalidRequestException("Invalid value '" + rawId + "' for ID. Must be a positive integer");
        }
    }
}
