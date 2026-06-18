package com.audio.auth.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${auth.server.url:http://localhost:9000}")
    private String authServerUrl;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(@RequestParam String code) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("auth-client", "secret");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", "http://127.0.0.1:8080/login/oauth2/code/auth-client");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                authServerUrl + "/oauth2/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("auth-client", "secret");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                authServerUrl + "/oauth2/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/client-credentials")
    public ResponseEntity<Map<String, Object>> getClientCredentialsToken() {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("storage-client", "storage-secret");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("scope", "read write");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                authServerUrl + "/oauth2/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspectToken(@RequestParam String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("auth-client", "secret");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                authServerUrl + "/oauth2/introspect",
                HttpMethod.POST,
                request,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeToken(@RequestParam String token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("auth-client", "secret");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                authServerUrl + "/oauth2/revoke",
                HttpMethod.POST,
                request,
                Void.class
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        String authUrl = authServerUrl + "/oauth2/authorize?" +
                "response_type=code&" +
                "client_id=auth-client&" +
                "redirect_uri=http://127.0.0.1:8080/auth/callback&" +
                "scope=openid%20profile%20roles";

        return ResponseEntity.ok(Map.of("authorization_url", authUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam String code) {
        return ResponseEntity.ok(Map.of("code", code, "next_step", "POST /auth/token with code"));
    }
}
