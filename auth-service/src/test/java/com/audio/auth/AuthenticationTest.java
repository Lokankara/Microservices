package com.audio.auth;

import com.audio.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles("test")
@Disabled
class AuthenticationTest {

    private static final String CLIENT_ID = "gateway";
    private static final String CLIENT_SECRET = "gateway-secret";
    private static final String GRANT_TYPE = "password";
    private static final String SCOPE = "openid profile roles";
    private static final String TOKEN_URL = "/auth/oauth2/token";
    private static final String USERNAME_ADMIN = "bob";
    private static final String PASSWORD_ADMIN = "bob";
    private static final String USERNAME_USER = "alice";
    private static final String PASSWORD_USER = "alice";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testAuthenticationWithBobCredentials() {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(USERNAME_ADMIN, PASSWORD_ADMIN);

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(USERNAME_ADMIN, authentication.getName());
    }

    @Test
    void testAuthenticationWithWrongPassword() {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(USERNAME_ADMIN, "wrongpassword");

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> {
            authenticationManager.authenticate(authenticationToken);
        });
    }

    @Test
    void testInvalidClientCredentials() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", "wrong-secret")
                        .param("username", USERNAME_ADMIN)
                        .param("password", PASSWORD_ADMIN)
                        .param("scope", SCOPE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTokenEndpointWithMissingParameters() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled("Password grant type requires custom OAuth2AuthenticationProvider (item #9 in fix plan)")
    void testObtainAccessTokenWithAdminRole() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", CLIENT_SECRET)
                        .param("username", USERNAME_ADMIN)
                        .param("password", PASSWORD_ADMIN)
                        .param("scope", SCOPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    @Disabled("Password grant type requires custom OAuth2AuthenticationProvider (item #9 in fix plan)")
    void testObtainAccessTokenWithUserRole() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", CLIENT_SECRET)
                        .param("username", USERNAME_USER)
                        .param("password", PASSWORD_USER)
                        .param("scope", SCOPE))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.token_type").value("bearer"))
                .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    @Disabled("Password grant type requires custom OAuth2AuthenticationProvider (item #9 in fix plan)")
    void testAccessTokenContainsRoles() throws Exception {
        MvcResult result = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", CLIENT_SECRET)
                        .param("username", USERNAME_ADMIN)
                        .param("password", PASSWORD_ADMIN)
                        .param("scope", SCOPE))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.parse(response)
                .read("$.access_token");

        var jwt = jwtDecoder.decode(accessToken);

        assertEquals(USERNAME_ADMIN, jwt.getSubject());
        assertNotNull(jwt.getClaimAsStringList("roles"));
        assertTrue(jwt.getClaimAsStringList("roles").contains(ROLE_ADMIN));
    }

    @Test
    @Disabled("Password grant type requires custom OAuth2AuthenticationProvider (item #9 in fix plan)")
    void testAccessTokenWithUserRoleContainsUserAuthority() throws Exception {
        MvcResult result = mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", CLIENT_SECRET)
                        .param("username", USERNAME_USER)
                        .param("password", PASSWORD_USER)
                        .param("scope", SCOPE))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String accessToken = com.jayway.jsonpath.JsonPath.parse(response)
                .read("$.access_token");

        var jwt = jwtDecoder.decode(accessToken);

        assertEquals(USERNAME_USER, jwt.getSubject());
        assertNotNull(jwt.getClaimAsStringList("roles"));
        assertTrue(jwt.getClaimAsStringList("roles").contains(ROLE_USER));
    }

    @Test
    @Disabled("Password grant type requires custom OAuth2AuthenticationProvider (item #9 in fix plan)")
    void testInvalidUserCredentials() throws Exception {
        mockMvc.perform(post(TOKEN_URL)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", GRANT_TYPE)
                        .param("client_id", CLIENT_ID)
                        .param("client_secret", CLIENT_SECRET)
                        .param("username", USERNAME_ADMIN)
                        .param("password", "wrongpassword")
                        .param("scope", SCOPE))
                .andExpect(status().isUnauthorized());
    }

}
