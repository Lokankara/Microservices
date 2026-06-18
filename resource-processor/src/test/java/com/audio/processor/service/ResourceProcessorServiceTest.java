package com.audio.processor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.audio.processor.dto.SongMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ResourceProcessorServiceTest {
    private static final Long RESOURCE_ID = 1L;
    private static final String NAME = "Song";
    private static final String ARTIST = "Artist";
    private static final String ALBUM = "Album";
    private static final String DURATION = "03:00";
    private static final String YEAR = "2023";
    private static final byte[] AUDIO_DATA = "fake audio data".getBytes();
    private static final String RESOURCE_PATH = "/resources/{id}";
    private static final String SONGS_PATH = "/songs";
    private static final String STREAM_BINDING = "resourceProcessed-out-0";
    private static final String SERVICE_ERROR_MESSAGE = "Service error";

    @Mock
    private WebClient resourceServiceClient;

    @Mock
    private Mp3MetadataExtractor metadataExtractor;

    @Mock
    private WebClient songServiceClient;

    @Mock
    private StreamBridge streamBridge;

    private ResourceProcessorService service;

    @BeforeEach
    void setUp() {
        service = new ResourceProcessorService(
                resourceServiceClient, songServiceClient, metadataExtractor, streamBridge);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void processValidResourceIdReturnsMetadata() {
        byte[] audioData = AUDIO_DATA;
        SongMetadata metadata = buildMetadata();

        WebClient.RequestHeadersUriSpec mockUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec mockHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);

        when(resourceServiceClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(RESOURCE_PATH, RESOURCE_ID)).thenReturn(mockHeadersSpec);
        when(mockHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(byte[].class)).thenReturn(Mono.just(audioData));
        when(metadataExtractor.extract(audioData)).thenReturn(metadata);

        WebClient.RequestBodyUriSpec mockBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec mockPostHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec mockPostResponseSpec = mock(WebClient.ResponseSpec.class);

        when(songServiceClient.post()).thenReturn(mockBodyUriSpec);
        when(mockBodyUriSpec.uri(SONGS_PATH)).thenReturn(mockBodyUriSpec);
        when(mockBodyUriSpec.bodyValue(metadata)).thenReturn(mockPostHeadersSpec);
        when(mockPostHeadersSpec.retrieve()).thenReturn(mockPostResponseSpec);
        when(mockPostResponseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        SongMetadata result = service.process(RESOURCE_ID);

        assertEquals(RESOURCE_ID, result.getId());
        assertEquals(NAME, result.getName());
        assertEquals(ARTIST, result.getArtist());
        assertEquals(ALBUM, result.getAlbum());
        assertEquals(DURATION, result.getDuration());
        assertEquals(YEAR, result.getYear());
        verify(streamBridge).send(eq(STREAM_BINDING), eq(RESOURCE_ID));
    }

    @Test
    void processWhenResourceServiceFailsThrowsException() {
        WebClient.RequestHeadersUriSpec mockUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec mockHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec mockResponseSpec = mock(WebClient.ResponseSpec.class);

        when(resourceServiceClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(RESOURCE_PATH, RESOURCE_ID)).thenReturn(mockHeadersSpec);
        when(mockHeadersSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.bodyToMono(byte[].class)).thenReturn(
                Mono.error(new RuntimeException(SERVICE_ERROR_MESSAGE)));

        assertThrows(RuntimeException.class, () -> service.process(RESOURCE_ID));
    }

    private SongMetadata buildMetadata() {
        return SongMetadata.builder()
                .name(NAME)
                .artist(ARTIST)
                .album(ALBUM)
                .duration(DURATION)
                .year(YEAR)
                .build();
    }
}
