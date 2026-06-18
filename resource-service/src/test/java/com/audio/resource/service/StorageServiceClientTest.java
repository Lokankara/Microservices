package com.audio.resource.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.audio.resource.dto.StorageResponse;
import com.audio.resource.entity.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class StorageServiceClientTest {

    private StorageServiceClient client;

    @BeforeEach
    void setUp() {
        StorageResponse.StubConfigProvider provider = new StorageResponse.StubConfigProvider(
                "test-staging", "/tmp/staging", "test-permanent", "/tmp/permanent");
        client = new StorageServiceClient("http://localhost:8085", provider);
    }

    @Test
    void getAllStoragesFallbackReturnsStubData() {
        StorageResponse response = client.getAllStoragesFallback(new RuntimeException("test"));
        assertNotNull(response);
        assertEquals(2, response.getStorages().size());
        assertTrue(response.isStubData());
    }

    @Test
    void stubDataHasCorrectStagingDetails() {
        StorageResponse stub = StorageResponse.stub();
        List<StorageResponse.StorageDto> storages = stub.getStorages();

        StorageResponse.StorageDto staging = storages.stream()
                .filter(s -> s.getStorageType() == StorageType.STAGING)
                .findFirst()
                .orElse(null);

        assertNotNull(staging);
        assertEquals(1L, staging.getId());
        assertEquals("staging-bucket", staging.getBucket());
        assertEquals("/staging", staging.getPath());
    }

    @Test
    void stubDataHasCorrectPermanentDetails() {
        StorageResponse stub = StorageResponse.stub();
        List<StorageResponse.StorageDto> storages = stub.getStorages();

        StorageResponse.StorageDto permanent = storages.stream()
                .filter(s -> s.getStorageType() == StorageType.PERMANENT)
                .findFirst()
                .orElse(null);

        assertNotNull(permanent);
        assertEquals(2L, permanent.getId());
        assertEquals("permanent-bucket", permanent.getBucket());
        assertEquals("/permanent", permanent.getPath());
    }

    @Test
    void stubDataRespectsCustomConfig() {
        StorageResponse customStub = StorageResponse.stub(
                new StorageResponse.StubConfig("custom-staging", "/custom/staging",
                        "custom-permanent", "/custom/permanent"));

        assertEquals("custom-staging", customStub.getStorages().get(0).getBucket());
        assertEquals("/custom/staging", customStub.getStorages().get(0).getPath());
        assertEquals("custom-permanent", customStub.getStorages().get(1).getBucket());
        assertEquals("/custom/permanent", customStub.getStorages().get(1).getPath());
    }

    @Test
    void getStoragesByTypeFallbackUsesInjectedConfig() {
        StorageResponse response = client.getStoragesByTypeFallback(StorageType.STAGING, new RuntimeException("boom"));

        assertNotNull(response);
        assertTrue(response.isStubData());
        assertEquals("test-staging", response.getStorages().get(0).getBucket());
        assertEquals("test-permanent", response.getStorages().get(1).getBucket());
    }
}
