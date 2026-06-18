package com.audio.song.service;

import com.audio.song.dto.SongRequest;
import com.audio.song.entity.SongEntity;
import com.audio.song.exception.DuplicateSongException;
import com.audio.song.exception.SongNotFoundException;
import com.audio.song.repository.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SongService {

    private final SongRepository songRepository;

    public SongService(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Transactional
    public Long createSong(SongRequest dto) {
        log.info("Creating song metadata: id={}, name={}, artist={}, album={}, duration={}, year={}", 
                dto.getId(), dto.getName(), dto.getArtist(), 
                dto.getAlbum(), dto.getDuration(), dto.getYear());

        if (songRepository.existsById(dto.getId())) {
            throw new DuplicateSongException("Metadata for this ID already exists");
        }

        SongEntity song = new SongEntity(
                dto.getId(),
                dto.getName(),
                dto.getArtist(),
                dto.getAlbum(),
                dto.getDuration(),
                dto.getYear()
        );

        SongEntity saved = songRepository.save(song);
        log.info("Successfully created song metadata with ID: {}", saved.getId());
        return saved.getId();
    }

    public SongRequest getSong(Long id) {
        log.info("Fetching song metadata for ID: {}", id);
        
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid song ID");
        }
        SongEntity song = songRepository.findById(id)
                .orElseThrow(() -> new SongNotFoundException("Song metadata for ID=" + id + " not found"));
        
        log.info("Found song metadata: id={}, name={}, artist={}", id, song.getName(), song.getArtist());
        return new SongRequest(
                song.getId(),
                song.getName(),
                song.getArtist(),
                song.getAlbum(),
                song.getDuration(),
                song.getYear()
        );
    }

    @Transactional
    public List<Long> deleteSongs(String idsCsv) {
        log.info("Deleting songs with IDs: {}", idsCsv);
        
        if (idsCsv == null || idsCsv.isEmpty()) {
            throw new IllegalArgumentException("ID list cannot be empty");
        }

        if (idsCsv.length() > 200) {
            throw new IllegalArgumentException("CSV string length must not exceed 200 characters");
        }

        List<Long> deletedIds = new ArrayList<>();
        String[] idParts = idsCsv.split(",");

        for (String idStr : idParts) {
            try {
                Long id = Long.parseLong(idStr.trim());
                if (id <= 0) {
                    continue;
                }
                if (songRepository.existsById(id)) {
                    songRepository.deleteById(id);
                    deletedIds.add(id);
                    log.debug("Deleted song with ID: {}", id);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid ID format: " + idStr);
            }
        }

        log.info("Successfully deleted {} songs", deletedIds.size());
        return deletedIds;
    }
}
