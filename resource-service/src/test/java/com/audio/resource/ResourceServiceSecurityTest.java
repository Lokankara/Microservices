package com.audio.resource;

import com.audio.resource.service.ResourceService;
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

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class ResourceServiceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ResourceService resourceService;

    @Test
    void missingTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/resources/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/resources"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/resources").param("id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRoleCanGetResource() throws Exception {
        when(resourceService.getById("1")).thenReturn(new byte[]{0, 1, 2});

        mockMvc.perform(get("/resources/1")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCanUploadResource() throws Exception {
        when(resourceService.upload(any())).thenReturn(null);

        mockMvc.perform(post("/resources")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("test audio content"))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCannotDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources").param("id", "1")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleCanGetResource() throws Exception {
        when(resourceService.getById("1")).thenReturn(new byte[]{0, 1, 2});

        mockMvc.perform(get("/resources/1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoleCanUploadResource() throws Exception {
        when(resourceService.upload(any())).thenReturn(null);

        mockMvc.perform(post("/resources")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("test audio content"))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoleCanDeleteResource() throws Exception {
        mockMvc.perform(delete("/resources").param("id", "1")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }
}
