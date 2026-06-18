package com.audio.song;

import com.audio.song.dto.SongRequest;
import com.audio.song.service.SongService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class SongServiceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private SongService songService;

    @Test
    void missingTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/songs/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"test\",\"artist\":\"test\",\"album\":\"test\",\"duration\":\"03:30\",\"year\":\"2024\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/songs").param("id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleCanGetSong() throws Exception {
        when(songService.getSong(1L)).thenReturn(new SongRequest());

        mockMvc.perform(get("/songs/1")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCannotCreateSong() throws Exception {
        mockMvc.perform(post("/songs")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"test\",\"artist\":\"test\",\"album\":\"test\",\"duration\":\"03:30\",\"year\":\"2024\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotDeleteSong() throws Exception {
        mockMvc.perform(delete("/songs").param("id", "1")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanGetSong() throws Exception {
        when(songService.getSong(1L)).thenReturn(new SongRequest());

        mockMvc.perform(get("/songs/1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoleCanCreateSong() throws Exception {
        when(songService.createSong(any())).thenReturn(1L);

        mockMvc.perform(post("/songs")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"test\",\"artist\":\"test\",\"album\":\"test\",\"duration\":\"03:30\",\"year\":\"2024\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoleCanDeleteSong() throws Exception {
        mockMvc.perform(delete("/songs").param("id", "1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }
}