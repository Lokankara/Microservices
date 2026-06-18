package com.audio.song.controller;

import com.audio.song.dto.SongCreateResponse;
import com.audio.song.dto.SongDeleteResponse;
import com.audio.song.dto.SongRequest;
import com.audio.song.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/songs")
@Slf4j
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SongCreateResponse> createSong(@Valid @RequestBody SongRequest dto) {
        Long id = songService.createSong(dto);
        log.info("POST /songs - Created song with ID: {}", id);
        return ResponseEntity.ok(new SongCreateResponse(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SongRequest> getSong(@PathVariable Long id) {
        SongRequest dto = songService.getSong(id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SongDeleteResponse> deleteSongs(@RequestParam String id) {
        log.info("DELETE /songs - Deleting songs with IDs: {}", id);
        List<Long> deletedIds = songService.deleteSongs(id);
        log.info("DELETE /songs - Deleted {} songs", deletedIds.size());
        return ResponseEntity.ok(new SongDeleteResponse(deletedIds));
    }
}
