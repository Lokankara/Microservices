package com.audio.resource.service;

import com.audio.resource.dto.ResourceDeleteResponse;
import com.audio.resource.dto.ResourceUploadResponse;
import com.audio.resource.dto.StorageResponse;
import com.audio.resource.entity.ResourceEntity;
import com.audio.resource.entity.StorageType;
import com.audio.resource.exception.InvalidRequestException;
import com.audio.resource.exception.ResourceNotFoundException;
import com.audio.resource.repository.ResourceRepository;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResourceServiceTest {

    @Mock
    private ResourceRepository repository;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private StorageServiceClient storageServiceClient;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ResourceEventPublisher eventPublisher;

    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(repository, songServiceClient, storageServiceClient, s3StorageService, eventPublisher);
    }

    @Test
    void uploadValidDataReturnsResponse() throws IOException {
        byte[] data = "audio data".getBytes();
        ServletInputStream servletInputStream = mock(ServletInputStream.class);
        when(servletInputStream.readAllBytes()).thenReturn(data);
        when(request.getInputStream()).thenReturn(servletInputStream);
        when(repository.save(any(ResourceEntity.class))).thenAnswer(inv -> {
            ResourceEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(s3StorageService.getUrl(any(String.class), any(String.class), anyString())).thenReturn("http://url");

        StorageResponse.StorageDto stagingDto = new StorageResponse.StorageDto(1L, StorageType.STAGING, "mp3-staging", "/uploads");
        StorageResponse stagingResponse = new StorageResponse();
        stagingResponse.setStorages(List.of(stagingDto));
        when(storageServiceClient.getStoragesByType(StorageType.STAGING)).thenReturn(stagingResponse);

        ResourceUploadResponse response = service.upload(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("http://url", response.getS3Url());
        verify(s3StorageService).upload(any(String.class), any(String.class), anyString(), any(byte[].class));
        verify(repository).save(any(ResourceEntity.class));
    }

    @Test
    void uploadFallsBackToStubDataWhenStorageServiceDown() throws IOException {
        byte[] data = "audio data".getBytes();
        ServletInputStream servletInputStream = mock(ServletInputStream.class);
        when(servletInputStream.readAllBytes()).thenReturn(data);
        when(request.getInputStream()).thenReturn(servletInputStream);
        when(repository.save(any(ResourceEntity.class))).thenAnswer(inv -> {
            ResourceEntity e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });
        when(s3StorageService.getUrl(any(String.class), any(String.class), anyString())).thenReturn("http://url");

        StorageResponse stub = StorageResponse.stub(
                new StorageResponse.StubConfig("custom-staging", "/tmp/staging",
                        "custom-permanent", "/tmp/permanent"));
        when(storageServiceClient.getStoragesByType(StorageType.STAGING)).thenReturn(stub);

        ResourceUploadResponse response = service.upload(request);

        assertNotNull(response);
        assertTrue(stub.isStubData());
        assertEquals("custom-staging", stub.getStorages().get(0).getBucket());
        verify(s3StorageService).upload(eq("custom-staging"), eq("/tmp/staging"), anyString(), any(byte[].class));
    }

    @Test
    void uploadWhenIOExceptionThrowsRuntimeException() throws IOException {
        when(request.getInputStream()).thenThrow(new IOException("IO error"));

        assertThrows(RuntimeException.class, () -> service.upload(request));
    }

    @Test
    void getByIdValidIdReturnsData() {
        long id = 1L;
        String storageKey = "key";
        String bucket = "mp3-staging";
        String path = "/uploads";
        ResourceEntity entity = new ResourceEntity(storageKey, StorageType.STAGING, bucket, path);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        byte[] data = "data".getBytes();
        when(s3StorageService.download(bucket, path, storageKey)).thenReturn(data);

        byte[] result = service.getById("1");

        assertEquals(data, result);
        verify(repository).findById(id);
        verify(s3StorageService).download(bucket, path, storageKey);
    }

    @Test
    void getByIdInvalidIdThrowsException() {
        assertThrows(InvalidRequestException.class, () -> service.getById("invalid"));
    }

    @Test
    void getByIdNotFoundThrowsException() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById("1"));
    }

    @ParameterizedTest
    @MethodSource("provideDeleteScenarios")
    void deleteValidIdsReturnsDeleted(String ids, String[] parts, long[] deletedIds) {
        for (String part : parts) {
            long id = Long.parseLong(part.trim());
            ResourceEntity entity = new ResourceEntity("key" + id, StorageType.STAGING, "mp3-staging", "/uploads");
            when(repository.findById(id)).thenReturn(Optional.of(entity));
        }

        ResourceDeleteResponse response = service.delete(ids);

        assertEquals(deletedIds.length, response.getIds().size());
        for (long id : deletedIds) {
            assertTrue(response.getIds().contains(id));
        }
    }

    private static Stream<Arguments> provideDeleteScenarios() {
        return Stream.of(
                Arguments.of("1,2", new String[]{"1", "2"}, new long[]{1, 2}),
                Arguments.of("1", new String[]{"1"}, new long[]{1})
        );
    }

    @Test
    void deleteNullIdsThrowsException() {
        assertThrows(InvalidRequestException.class, () -> service.delete(null));
    }

    @Test
    void deleteTooLongIdsThrowsException() {
        String longIds = "1,".repeat(101);
        assertThrows(InvalidRequestException.class, () -> service.delete(longIds));
    }

    @Test
    void deleteInvalidIdFormatThrowsException() {
        assertThrows(InvalidRequestException.class, () -> service.delete("abc"));
    }

    @ParameterizedTest
    @MethodSource("provideValidateIdScenarios")
    void validateId(String rawId, long expected) {
        long result = service.validateId(rawId);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideValidateIdScenarios() {
        return Stream.of(
                Arguments.of("1", 1L),
                Arguments.of("123", 123L)
        );
    }

    @Test
    void validateIdInvalidThrowsException() {
        assertThrows(InvalidRequestException.class, () -> service.validateId("0"));
        assertThrows(InvalidRequestException.class, () -> service.validateId("-1"));
        assertThrows(InvalidRequestException.class, () -> service.validateId("abc"));
    }
}
