# OAuth2 Security Integration Code Review Implementation Plan

## Summary
This plan addresses security issues, bugs, and improvements identified in the OAuth2 Authorization Server integration across auth-service, gateway, song-service, storage-service, resource-service, and ui-service.

## Critical Issues (Must Fix)

### 1. Fix JWKS Endpoint and Issuer URI in Config Files
**Files:** `config-repo/gateway.yml`, `config-repo/resource-service.yml`, `config-repo/resource-service-docker.yml`, `config-repo/song-service-docker.yml`, `config-repo/storage-service.yml`, `config-repo/storage-service-docker.yml`

**Problem:** Incorrect `jwk-set-uri` paths using `/.well-known/jwks.json` instead of `/oauth2/jwks`. The issuer URI has `/auth` suffix but auth-service sets issuer as `http://localhost:9000` (without /auth), causing JWT validation failures.

**Fix:**
- Change `jwk-set-uri` from `/auth/.well-known/jwks.json` to `/auth/oauth2/jwks` (for docker files) or `/oauth2/jwks` (for local files based on how the app is accessed)
- Update issuer-uri to match auth-service.Issuer claim (`http://localhost:9000` for local, `http://auth-service:9000` for docker)

### 2. Fix OAuth2 Endpoint Paths in Auth Service SecurityConfig
**File:** `auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java`

**Problem:** Lines 143-144 have `/auth/oauth2/authorize` and `/auth/oauth2/token` but with `server.servlet.context-path=/auth`, the servlet container prepends `/auth`, creating double-prefix paths like `/auth/auth/oauth2/token`.

**Fix:**
- Remove `/auth` prefix from endpoint paths: use `/oauth2/authorize` and `/oauth2/token`
- Update line 55: Change `/auth/oauth2/**` to `/oauth2/**`

### 3. Implement JWT Role Claim Converter in Resource Services
**Files:** `resource-service/src/main/java/com/audio/resource/config/SecurityConfig.java`, `song-service/src/main/java/com/audio/song/config/SecurityConfig.java`, `storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java`

**Problem:** All three services use `Customizer.withDefaults()` for JWT which expects `scope`/`scp` claims, but auth-service emits a custom `roles` claim. This causes `@PreAuthorize("hasRole('ADMIN')")` checks to fail with 403.

**Fix:** Add `JwtAuthenticationConverter` bean to each SecurityConfig that maps the `roles` claim to `ROLE_*` authorities (same pattern already implemented in gateway).

### 4. Add CORS Configuration to storage-service SecurityConfig
**File:** `storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java`

**Problem:** Missing `CorsConfigurationSource` bean - song-service has it but storage-service only uses `.cors(Customizer.withDefaults())` which won't allow requests from the React UI at `http://localhost:3000`.

**Fix:** Add `CorsConfigurationSource` bean with allowed origins `http://localhost:3000`.

### 5. Fix POST /storages to Return 201 Created
**File:** `storage-service/src/main/java/com/audio/storage/controller/StorageController.java`

**Problem:** Line 33 returns `ResponseEntity.ok()` (200) instead of `ResponseEntity.status(HttpStatus.CREATED)` (201) for resource creation.

**Fix:** Change to return `HttpStatus.CREATED`.

## Major Security Issues (Must Fix)

### 6. Persist RSA Signing Keys
**File:** `auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java`

**Problem:** Lines 99-121 generate ephemeral RSA keys on every startup, invalidating previously issued tokens and causing issues in multi-instance deployments.

**Recommendation:** This is a significant architectural change. Consider externalizing keys to a keystore or mounted volume. However, for development purposes, this may be acceptable. **Decision needed:** Is persistence required or can this be deferred?

### 7. Profile-Gate DataInitializer Seeding
**File:** `auth-service/src/main/java/com/audio/auth/config/DataInitializer.java`

**Problem:** Unconditionally seeds users with predictable passwords when table is empty.

**Fix:** Add `@Profile("dev")` or similar conditional to only seed in development environments.

### 8. Hash OAuth Client Secret
**File:** `auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java`

**Problem:** Line 77 uses `{noop}gateway-secret` storing plaintext.

**Fix:** Use `{bcrypt}` prefix or encode via injected `PasswordEncoder`.

### 9. Implement Password Grant Type Support
**File:** `auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java`

**Problem:** Line 81 registers password grant but Spring Authorization Server requires custom `OAuth2AuthenticationConverter` and `OAuth2AuthenticationProvider` to actually support it.

**Recommendation:** This is complex. Consider using Authorization Code flow with PKCE for the SPA instead, or implement the custom grant infrastructure. **Decision needed:** Preferred approach?

### 10. Remove Client Secret from UI Service
**File:** `ui-service/src/services/authService.js`

**Problem:** Line 11 embeds `client_secret` in browser-deliverable JavaScript. Client secrets should never be in frontend code.

**Recommendation:** Implement Authorization Code + PKCE flow or use a public client configuration. This is a significant refactor.

### 11. Fix React Build Output Path in build.gradle
**File:** `ui-service/build.gradle`

**Problem:** `buildReactApp` task outputs to `build/resources/main/static` but React's default `build` script outputs to `./build/`.

**Fix:** Add `environment "BUILD_PATH", layout.buildDirectory.dir("resources/main/static").get().asFile.absolutePath` to the NpmTask configuration.

### 12. Fix Hardcoded API URLs in UI Service
**Files:** `ui-service/src/services/authService.js`, `ui-service/src/services/axiosInstance.js`

**Problem:** Both files hardcode `localhost` URLs which break in Docker/prod.

**Fix:** Use environment variables `REACT_APP_AUTH_BASE_URL` and `REACT_APP_API_BASE_URL`.

### 13. Run Container as Non-Root User
**File:** `auth-service/Dockerfile`

**Problem:** No USER directive - container runs as root.

**Fix:** Add non-root user creation and USER directive.

## Minor Fixes (Quick Wins)

### 14. Update Test Constants for Role Assertions
**File:** `auth-service/src/test/java/com/audio/auth/AuthenticationTest.java`

**Problem:** Lines 41-42 define `ROLE_ADMIN` and `ROLE_USER` with "ROLE_" prefix, but token customizer emits roles without prefix (lines 132-134).

**Fix:** Remove "ROLE_" prefix from test constants to match actual token output.

### 15. Enable Core Token Tests
**File:** `auth-service/src/test/java/com/audio/auth/AuthenticationTest.java`

**Problem:** Lines 116-214 have `@Disabled` on critical token issuance tests.

**Fix:** Remove `@Disabled` from at least one happy-path test to ensure CI validates token creation.

### 16. Fix StoragesTable Functional State Updates
**File:** `ui-service/src/components/StoragesTable.js`

**Problem:** Lines 34 and 44 use closure-captured `storages` which can cause race conditions.

**Fix:** Use functional updates: `setStorages(prev => [...prev, response.data])`.

### 17. Fix Stylelint Font-Family Quotes in CSS
**File:** `ui-service/src/index.css`

**Problem:** Lines 3-4 quote single-word font names (Roboto, Oxygen, Ubuntu, Cantarell).

**Fix:** Remove quotes from single-word font family names.

### 18. Fix App.js Login Route Logic
**File:** `ui-service/src/App.js`

**Problem:** Line 10 renders login form when `path === '/login'` even if authenticated.

**Fix:** Restructure logic to only show login when not authenticated, redirect authenticated users from `/login` to `/dashboard`.

### 19. Add Mock Data in SongServiceSecurityTest
**File:** `song-service/src/test/java/com/audio/song/SongServiceSecurityTest.java`

**Problem:** Lines 50-55 and 73-78 don't mock `songService.getSong()` return value.

**Fix:** Add `when(songService.getSong(1L)).thenReturn(...)` for relevant tests.

### 20. Fix Postman Test Password
**File:** `tools/api-tests/module8-security-tests.json`

**Problem:** Line 23 has `password123` but actual password is `alice`.

**Fix:** Change to `"alice"`.

## Out-of-Scope / Deferred

### jvm-metrics.json
- File not found in the repository (only `gateway-metrics.json` exists)
- Cannot apply suggested fixes

### .env CONFIG_REPO_URI
- Points to external mutable repo
- Acceptable for local development but should use org-controlled repo in production
- **Recommendation:** Document this as a deployment consideration rather than code fix

## Implementation Order Recommendation

1. **Critical fixes first** (JWKS/issuer, OAuth2 endpoints, JWT converters, CORS)
2. **Fix API response codes** (POST /storages → 201)
3. **Security hardening** (non-root user, profile-gated seeding, client secret hash)
4. **UI fixes** (build path, environment variables, React state updates, App.js logic)
5. **Test fixes** (enable disabled tests, fix test assertions)
6. **External/system considerations** (CONFIG_REPO_URI, persistent keys, password grant) - require decisions
