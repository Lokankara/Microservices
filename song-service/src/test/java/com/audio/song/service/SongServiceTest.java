package com.audio.song.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audio.song.dto.SongRequest;
import com.audio.song.entity.SongEntity;
import com.audio.song.exception.DuplicateSongException;
import com.audio.song.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    @ParameterizedTest
    @MethodSource("provideValidSongRequests")
    void createSongWhenValidInputThenReturnsId(SongRequest dto) {
        when(songRepository.existsById(dto.getId())).thenReturn(false);
        when(songRepository.save(any(SongEntity.class))).thenAnswer(i -> i.getArgument(0));

        Long id = songService.createSong(dto);

        assertEquals(dto.getId(), id);
        verify(songRepository).save(argThat(s ->
                s.getId().equals(dto.getId()) &&
                        s.getName().equals(dto.getName()) &&
                        s.getArtist().equals(dto.getArtist()) &&
                        s.getAlbum().equals(dto.getAlbum()) &&
                        s.getDuration().equals(dto.getDuration()) &&
                        s.getYear().equals(dto.getYear())
        ));
    }

    private static Stream<Arguments> provideValidSongRequests() {
        return Stream.of(
                Arguments.of(SongRequest.builder()
                        .id(1L)
                        .name("We are the champions")
                        .artist("Queen")
                        .album("News of the world")
                        .duration("02:59")
                        .year("1977")
                        .build()),
                Arguments.of(SongRequest.builder()
                        .id(2L)
                        .name("Another Song")
                        .artist("Another Artist")
                        .album("Another Album")
                        .duration("03:45")
                        .year("1980")
                        .build())
        );
    }

    @Test
    void createSongWhenDuplicateIdThenThrowsException() {
        SongRequest dto = SongRequest.builder()
                .id(1L)
                .name("Song")
                .artist("Artist")
                .album("Album")
                .duration("03:00")
                .year("2020")
                .build();

        when(songRepository.existsById(1L)).thenReturn(true);

        DuplicateSongException ex = assertThrows(DuplicateSongException.class, () -> songService.createSong(dto));
        assertEquals("Metadata for this ID already exists", ex.getMessage());
    }

    @Test
    void getSongWhenValidIdThenReturnsDto() {
        SongEntity song = new SongEntity(1L, "Song", "Artist", "Album", "03:00", "2020");
        when(songRepository.findById(1L)).thenReturn(Optional.of(song));

        SongRequest dto = songService.getSong(1L);

        assertEquals(1, dto.getId());
        assertEquals("Song", dto.getName());
        assertEquals("Artist", dto.getArtist());
        assertEquals("Album", dto.getAlbum());
        assertEquals("03:00", dto.getDuration());
        assertEquals("2020", dto.getYear());
    }

    @Test
    void getSongWhenInvalidIdThenThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> songService.getSong(0L));
        assertEquals("Invalid song ID", ex.getMessage());
    }

    @Test
    void deleteSongsWhenValidCsvThenReturnsIds() {
        when(songRepository.existsById(1L)).thenReturn(true);
        when(songRepository.existsById(2L)).thenReturn(true);
        doNothing().when(songRepository).deleteById(anyLong());

        List<Long> deleted = songService.deleteSongs("1,2");

        assertEquals(2, deleted.size());
        verify(songRepository).deleteById(1L);
        verify(songRepository).deleteById(2L);
    }

    @Test
    void deleteSongsWhenEmptyCsvThenThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> songService.deleteSongs(""));
        assertEquals("ID list cannot be empty", ex.getMessage());
    }

    @Test
    void deleteSongsWhenInvalidFormatThenThrowsException() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> songService.deleteSongs("xyz"));
        assertTrue(ex.getMessage().contains("Invalid ID format"));
    }

    @Test
    void deleteSongsWhenNonExistentIdsThenIgnored() {
        when(songRepository.existsById(anyLong())).thenReturn(false);
        List<Long> deleted = songService.deleteSongs("999,1000");
        assertTrue(deleted.isEmpty());
        verify(songRepository, never()).deleteById(anyLong());
    }
}
