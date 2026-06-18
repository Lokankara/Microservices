package com.audio.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RefreshScope
public class S3StorageService {

    private final S3Client s3Client;
    private final String endpoint;

    public S3StorageService(S3Client s3Client,
            @Value("${spring.cloud.aws.s3.endpoint:http://localhost:4566}") String endpoint) {
        this.s3Client = s3Client;
        this.endpoint = endpoint;
    }

    public void upload(String bucket, String path, String key, byte[] data) {
        String fullKey = fullKey(path, key);
        log.info("Uploading to bucket={}, path={}, key={}", bucket, path, fullKey);
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(fullKey).build(),
                RequestBody.fromBytes(data)
        );
    }

    public byte[] download(String bucket, String path, String key) {
        String fullKey = fullKey(path, key);
        log.info("Downloading from bucket={}, path={}, key={}", bucket, path, fullKey);
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(fullKey).build()
        ).asByteArray();
    }

    public String getUrl(String bucket, String path, String key) {
        String fullKey = fullKey(path, key);
        return String.format("%s/%s/%s", endpoint, bucket, fullKey);
    }

    public void delete(String bucket, String path, String key) {
        String fullKey = fullKey(path, key);
        log.info("Deleting from bucket={}, path={}, key={}", bucket, path, fullKey);
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(fullKey).build()
        );
    }

    public void move(String sourceBucket, String sourcePath, String destBucket, String destPath, String key) {
        byte[] data = download(sourceBucket, sourcePath, key);
        upload(destBucket, destPath, key, data);
        delete(sourceBucket, sourcePath, key);
        log.info("Moved file '{}' from {}/{} to {}/{}", key, sourceBucket, sourcePath, destBucket, destPath);
    }

    private String fullKey(String path, String key) {
        return path.startsWith("/") ? path.substring(1) + "/" + key : path + "/" + key;
    }
}
