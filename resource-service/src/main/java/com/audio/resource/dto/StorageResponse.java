package com.audio.resource.dto;

import com.audio.resource.entity.StorageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageResponse {
    @JsonProperty("storages")
    private List<StorageDto> storages;

    @JsonIgnore
    private boolean stubData = false;

    public static StorageResponse stub() {
        return stub(StubConfig.defaults());
    }

    public static StorageResponse stub(StubConfig config) {
        StorageResponse response = new StorageResponse();
        response.setStubData(true);
        response.setStorages(List.of(
                new StorageDto(1L, StorageType.STAGING, config.stagingBucket(), config.stagingPath()),
                new StorageDto(2L, StorageType.PERMANENT, config.permanentBucket(), config.permanentPath())
        ));
        return response;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageDto {
        private Long id;
        private StorageType storageType;
        private String bucket;
        private String path;
    }

    public record StubConfig(String stagingBucket,
                             String stagingPath,
                             String permanentBucket,
                             String permanentPath) {
        public static StubConfig defaults() {
            return new StubConfig("staging-bucket", "/staging", "permanent-bucket", "/permanent");
        }
    }

    @Getter
    @Component
    public static class StubConfigProvider {
        private final StubConfig stubConfig;

        public StubConfigProvider(
                @Value("${storage.fallback.staging-bucket:staging-bucket}") String stagingBucket,
                @Value("${storage.fallback.staging-path:/staging}") String stagingPath,
                @Value("${storage.fallback.permanent-bucket:permanent-bucket}") String permanentBucket,
                @Value("${storage.fallback.permanent-path:/permanent}") String permanentPath) {
            this.stubConfig = new StubConfig(stagingBucket, stagingPath, permanentBucket, permanentPath);
        }
    }
}
