package com.audio.storage.dto;

import com.audio.storage.entity.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageCreateRequest {

    @NotNull(message = "storageType must not be null")
    private StorageType storageType;

    @NotBlank(message = "bucket must not be blank")
    private String bucket;

    @NotBlank(message = "path must not be blank")
    private String path;
}
