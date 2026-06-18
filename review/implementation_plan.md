# Implementation Plan

## 1. auth-service (Security & Configuration)
- [ ] **Ephemeral Keys Issue** (SecurityConfig.java lines 99-109)
  - Replace `generateRsaKey()` with key loading from keystore/KMS
  - Implement key rotation strategy
  - Update Dockerfile to use non-root user
- [ ] **Plaintext Client Secret** (SecurityConfig.java line 77)
  - Remove `{noop}` prefix
  - Apply bcrypt encoding via `PasswordEncoder` bean
- [ ] **Exposed Credentials** (DataInitializer.java lines 18-31)
  - Add `@Profile("dev")` to `DataInitializer`
  - Externalize passwords via `@Value`
- [ ] **Password Grant Type** (SecurityConfig.java line 81)
  - Implement custom `OAuth2AuthenticationConverter`
  - Create custom `OAuth2AuthenticationProvider`
  - Configure token endpoint with these components
- [ ] **Testing Gaps** (AuthenticationTest.java lines 116-214)
  - Enable at least one happy-path token test
  - Re-enable role claim validation tests
- [ ] **Docker Security** (application-docker.yml lines 10-12)
  - Replace hardcoded DB credentials with env vars
  - Set security logging to `INFO`
- [ ] **Frontend Auth Flow** (authService.js lines 7-11)
  - Remove `client_secret` from frontend
  - Implement Authorization Code + PKCE flow
- [ ] **Axios Base URL** (axiosInstance.js lines 4-6)
  - Replace localhost with env var
- [ ] **Actuator Endpoints** (SecurityConfig.java lines 24-25)
  - Restrict `/actuator/**` to authenticated users
  - Explicitly permit only health/info endpoints

## 2. ui-service (Frontend)
- [ ] **Client Secret Exposure** (authService.js lines 7-11)
  - Remove `client_secret` from `params.append`
  - Implement BFF token exchange
- [ ] **Axios Base URL** (axiosInstance.js lines 4-6)
  - Use `process.env.REACT_APP_API_BASE_URL`
- [ ] **Login Form Logic** (App.js lines 10-14)
  - Remove path check for `/login`
  - Add redirect for authenticated users
- [ ] **State Management** (StoragesTable.js lines 34-44)
  - Replace closure-captured state with functional updates
- [ ] **CSS Formatting** (index.css lines 3-4)
  - Remove quotes around single-word fonts

## 3. gateway (Security)
- [ ] **Actuator Endpoints** (SecurityConfig.java lines 24-25)
  - Implement granular `pathMatchers` for `/actuator/**`
- [ ] **JWT Configuration** (SecurityConfig.java lines 34-41)
  - Add `JwtAuthenticationConverter` for roles claim

## 4. storage-service (Security & CORS)
- [ ] **JWT Authority Mapping** (SecurityConfig.java lines 23-25)
  - Implement custom `JwtGrantedAuthoritiesConverter`
- [ ] **CORS Configuration** (SecurityConfig.java lines 17-30)
  - Add `CorsConfigurationSource` bean for `http://localhost:3000`

## 5. config-repo (Infrastructure)
- [ ] **Docker Credentials** (auth-service-docker.yml lines 9-13)
  - Replace hardcoded DB creds with env vars
  - Set security logging to `INFO`
- [ ] **Gateway Config** (gateway.yml lines 39-40)
  - Fix `issuer-uri` and `jwk-set-uri` paths
- [ ] **Resource Service Config** (resource-service.yml lines 33-34)
  - Update `jwk-set-uri` to `/oauth2/jwks`
- [ ] **Script Compatibility** (run_test.sh)
  - Convert to Unix shell syntax or rename to `.bat`

## 6. api-tests (Testing)
- [ ] **Test Data** (module8-security-tests.json lines 20-26)
  - Update alice password to "alice"

## 7. build.gradle (ui-service)
- [ ] **Build Path Mismatch** (build.gradle lines 19-21)
  - Add `BUILD_PATH` env var or add copy task
</write_to_file>