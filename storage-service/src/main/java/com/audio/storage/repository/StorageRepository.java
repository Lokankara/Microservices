package com.audio.storage.repository;

import com.audio.storage.entity.Storage;
import com.audio.storage.entity.StorageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {

    Optional<Storage> findByStorageType(StorageType storageType);

    boolean existsByStorageTypeAndBucketAndPath(StorageType storageType, String bucket, String path);
}
