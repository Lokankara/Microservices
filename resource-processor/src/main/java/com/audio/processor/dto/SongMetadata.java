package com.audio.processor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongMetadata {
    private Long id;
    private String name;
    private String artist;
    private String album;
    private String duration;
    private String year;
}
