package com.audio.storage.service;

import com.audio.storage.dto.StorageCreateRequest;
import com.audio.storage.dto.StorageCreateResponse;
import com.audio.storage.dto.StorageResponse;
import com.audio.storage.entity.Storage;
import com.audio.storage.repository.StorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StorageService {

    private final StorageRepository storageRepository;

    @Autowired
    public StorageService(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
    }

    @Transactional
    public StorageCreateResponse create(StorageCreateRequest request) {
        if (storageRepository.existsByStorageTypeAndBucketAndPath(
                request.getStorageType(), request.getBucket(), request.getPath())) {
            throw new IllegalArgumentException("Storage with type=" + request.getStorageType()
                    + ", bucket=" + request.getBucket() + ", path=" + request.getPath() + " already exists");
        }

        Storage storage = new Storage(request.getStorageType(), request.getBucket(), request.getPath());
        Storage saved = storageRepository.save(storage);
        log.info("Created storage: id={}, type={}, bucket={}, path={}",
                saved.getId(), saved.getStorageType(), saved.getBucket(), saved.getPath());
        return new StorageCreateResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public List<StorageResponse> getAll() {
        return storageRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Long> deleteByIds(String csvIds) {
        if (csvIds == null || csvIds.isBlank()) {
            throw new IllegalArgumentException("CSV string must not be null or blank");
        }
        if (csvIds.length() > 200) {
            throw new IllegalArgumentException("CSV string is too long: received "
                    + csvIds.length() + " characters, maximum allowed is 200");
        }

        String[] parts = csvIds.split(",");
        List<Long> deletedIds = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            try {
                long id = Long.parseLong(trimmed);
                if (id <= 0) {
                    throw new NumberFormatException();
                }
                Optional<Storage> storageOpt = storageRepository.findById(id);
                if (storageOpt.isPresent()) {
                    storageRepository.deleteById(id);
                    deletedIds.add(id);
                    log.info("Deleted storage: id={}", id);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ID format: '" + trimmed
                        + "'. Only positive integers are allowed");
            }
        }

        return deletedIds;
    }

    private StorageResponse toResponse(Storage storage) {
        return new StorageResponse(storage.getId(), storage.getStorageType(),
                storage.getBucket(), storage.getPath());
    }
}
