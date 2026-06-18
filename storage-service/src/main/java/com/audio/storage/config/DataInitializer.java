package com.audio.storage.config;

import com.audio.storage.entity.Storage;
import com.audio.storage.entity.StorageType;
import com.audio.storage.repository.StorageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final StorageRepository storageRepository;
    private final S3Client s3Client;

    @Value("${storage.staging.bucket:staging-bucket}")
    private String stagingBucket;

    @Value("${storage.staging.path:/staging}")
    private String stagingPath;

    @Value("${storage.permanent.bucket:permanent-bucket}")
    private String permanentBucket;

    @Value("${storage.permanent.path:/permanent}")
    private String permanentPath;

    public DataInitializer(StorageRepository storageRepository, S3Client s3Client) {
        this.storageRepository = storageRepository;
        this.s3Client = s3Client;
    }

    @PostConstruct
    void probeS3Connectivity() {
        try {
            s3Client.listBuckets();
            log.info("S3 health probe OK");
        } catch (Exception e) {
            log.warn("S3 health probe FAILED at startup: {}. Bucket creation may fail.", e.getMessage());
        }
    }

    @Override
    public void run(String... args) {
        createBucketIfNotExists(stagingBucket);
        createBucketIfNotExists(permanentBucket);

        createStorageIfNotExists(StorageType.STAGING, stagingBucket, stagingPath);
        createStorageIfNotExists(StorageType.PERMANENT, permanentBucket, permanentPath);
    }

    private void createStorageIfNotExists(StorageType type, String bucket, String path) {
        if (!storageRepository.existsByStorageTypeAndBucketAndPath(type, bucket, path)) {
            Storage storage = new Storage(type, bucket, path);
            storageRepository.save(storage);
            log.info("Pre-created storage: type={}, bucket={}, path={}", type, bucket, path);
        } else {
            log.info("Storage already exists: type={}, bucket={}, path={}", type, bucket, path);
        }
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket already exists: {}", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                try {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    log.info("Created bucket: {}", bucketName);
                } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ex) {
                    log.info("Bucket already exists (concurrent creation): {}", bucketName);
                } catch (S3Exception createEx) {
                    log.error("Failed to create bucket {}: {}", bucketName, createEx.awsErrorDetails().errorMessage());
                    throw createEx;
                }
            } else {
                log.error("Failed to access bucket {}: {}", bucketName, e.awsErrorDetails().errorMessage());
                throw e;
            }
        }
    }
}
