package com.audio.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongRequest {
    @NotNull(message = "ID is required")
    private Long id;

    @NotBlank(message = "Song name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Artist name is required")
    @Size(min = 1, max = 100, message = "Artist must be between 1 and 100 characters")
    private String artist;

    @NotBlank(message = "Album name is required")
    @Size(min = 1, max = 100, message = "Album must be between 1 and 100 characters")
    private String album;

    @NotBlank(message = "Duration is required")
    @Pattern(regexp = "^\\d{2}:(0[0-9]|[1-5][0-9])$", message = "Duration must be in mm:ss format with leading zeros")
    private String duration;

    @NotBlank(message = "Year is required")
    @Pattern(regexp = "^(19|20)\\d{2}$", message = "Year must be between 1900 and 2099")
    private String year;
}
