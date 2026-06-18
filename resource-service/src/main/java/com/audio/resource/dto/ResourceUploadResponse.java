package com.audio.resource.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUploadResponse {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("s3Url")
    private String s3Url;
}
