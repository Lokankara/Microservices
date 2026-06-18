package com.audio.resource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3StorageServiceTest {

    @Mock
    ResponseBytes<GetObjectResponse> responseBytes;
    @Mock
    private S3Client s3Client;

    private S3StorageService service;

    private final String endpoint = "http://localhost:4566";
    private final String bucket = "test-bucket";
    private final String path = "/uploads";
    private final String key = "test-key";


    @BeforeEach
    void setUp() {
        service = new S3StorageService(s3Client, endpoint);
    }

    @Test
    void uploadStoresDataInS3() {
        byte[] data = "test data".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(
                PutObjectResponse.builder().build());

        service.upload(bucket, path, key, data);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void downloadRetrievesDataFromS3() {
        byte[] expectedData = "test data".getBytes();
        when(responseBytes.asByteArray()).thenReturn(expectedData);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = service.download(bucket, path, key);

        assertEquals(expectedData, result);
        verify(s3Client).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void getUrlReturnsCorrectUrl() {
        String expectedUrl = endpoint + "/" + bucket + "/uploads/" + key;

        String result = service.getUrl(bucket, path, key);

        assertEquals(expectedUrl, result);
    }

    @Test
    void deleteRemovesDataFromS3() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());
        service.delete(bucket, path, key);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
