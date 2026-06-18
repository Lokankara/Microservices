package com.audio.song.service;

import com.audio.song.dto.SongRequest;
import com.audio.song.dto.SongResponse;
import com.audio.song.entity.SongEntity;
import org.springframework.stereotype.Component;

@Component
public class SongMapper {

    public SongEntity toEntity(SongRequest request) {
        SongEntity entity = new SongEntity();
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setArtist(request.getArtist());
        entity.setAlbum(request.getAlbum());
        entity.setDuration(request.getDuration());
        entity.setYear(request.getYear());
        return entity;
    }

    public SongResponse toResponse(SongEntity entity) {
        SongResponse response = new SongResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setArtist(entity.getArtist());
        response.setAlbum(entity.getAlbum());
        response.setDuration(entity.getDuration());
        response.setYear(entity.getYear());
        return response;
    }
}
