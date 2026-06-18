This PR introduces a new auth-service Spring Boot OAuth2 Authorization Server with JPA-backed users, RSA-signed JWTs, and a custom roles claim. JWT resource-server security is added to the gateway, resource-service, song-service, and storage-service. A React ui-service with login/dashboard/storages management is added. Config-repo YAML files, compose.yaml, and .env are updated to wire auth infrastructure. Logback is updated across all services for structured tracing, and new Grafana dashboards are added.

Changes
OAuth2 Security Integration

Layer / File(s)	Summary
Auth service domain model and data access
auth-service/src/main/java/com/audio/auth/entity/User.java, auth-service/src/main/java/com/audio/auth/repository/UserRepository.java, auth-service/src/main/java/com/audio/auth/service/CustomUserDetailsService.java, auth-service/src/main/java/com/audio/auth/config/DataInitializer.java	User JPA entity with eager user_roles collection; UserRepository with findByUsername; CustomUserDetailsService mapping roles to ROLE_-prefixed authorities; DataInitializer seeding alice (USER) and bob (ADMIN) when the repository is empty.
Auth service security config and application setup
auth-service/build.gradle, auth-service/src/main/java/com/audio/auth/AuthServiceApplication.java, auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java, auth-service/src/main/resources/application*.yml, auth-service/src/main/resources/logback-spring.xml, auth-service/Dockerfile, auth-service/.dockerignore	SecurityConfig configures OAuth2 Authorization Server with OIDC, in-memory gateway registered client, RSA JWK source, JWT token customizer emitting roles claim, delegating PasswordEncoder, and DaoAuthenticationProvider; H2 (local) and PostgreSQL (Docker) application profiles; multi-stage Dockerfile; logback with LOGSTASH JSON appender.
Auth service integration tests
auth-service/src/test/java/com/audio/auth/AuthServiceApplicationTest.java, auth-service/src/test/java/com/audio/auth/AuthenticationTest.java, auth-service/src/test/resources/application-test.yml	Context-load test; AuthenticationTest covering AuthenticationManager success/failure, token endpoint 401 for invalid client secret and 400 for missing params, and disabled JWT claim assertion tests; test profile with H2.
Resource service JWT resource-server security
resource-service/build.gradle, resource-service/src/main/java/com/audio/resource/config/SecurityConfig.java, resource-service/src/main/java/com/audio/resource/controller/ResourceController.java, resource-service/src/main/java/com/audio/resource/exception/GlobalExceptionHandler.java, resource-service/src/main/java/com/audio/resource/messaging/ResourceEventConsumer.java, resource-service/src/test/java/com/audio/resource/ResourceServiceSecurityTest.java	SecurityConfig enables JWT resource server (actuator permit-all); @PreAuthorize on upload/get (authenticated) and delete (ADMIN); AuthorizationDeniedException → 403 handler; processedResource @Bean consumer removed; security test verifying 401/403/200 per role.
Song and Storage service JWT resource-server security
song-service/build.gradle, song-service/src/main/java/com/audio/song/config/SecurityConfig.java, song-service/src/main/java/com/audio/song/controller/SongController.java, song-service/src/main/java/com/audio/song/exception/GlobalExceptionHandler.java, song-service/src/test/..., storage-service/build.gradle, storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java, storage-service/src/main/java/com/audio/storage/controller/StorageController.java, storage-service/src/main/java/com/audio/storage/exception/GlobalExceptionHandler.java	Both services gain SecurityConfig (JWT resource server, CORS), @PreAuthorize role enforcement on all CRUD endpoints, 403 AuthorizationDeniedException handlers, and security test suites verifying 401/403/200 behavior.
Gateway WebFlux security and JWT converter
gateway/build.gradle, gateway/src/main/java/com/audio/gateway/config/SecurityConfig.java, gateway/src/main/resources/logback-spring.xml	Gateway switches to spring-cloud-gateway-server-webflux; SecurityWebFilterChain permits /actuator/** and enforces JWT auth on all other routes; ReactiveJwtAuthenticationConverter maps roles claim to ROLE_ authorities.
Config repo YAMLs, compose, and environment wiring
.env, .gitignore, compose.yaml, config-repo/auth-*.yml, config-repo/gateway.yml, config-repo/resource-service*.yml, config-repo/song-service*.yml, config-repo/storage-service*.yml, config-service/gradle.properties, config-service/src/main/resources/application.yaml, README.md	Auth-service config-repo profiles (local PostgreSQL port 5435, Docker auth-db); issuer-uri/jwk-set-uri JWT settings added to all resource-service configs and gateway; compose.yaml adds auth-db (PostgreSQL + healthcheck) and auth-service containers; .env adds auth DB vars, changes Grafana port to 3090, adds CONFIG_REPO_URI; config-service git URI updated to GitHub; README expanded with startup order and health-check endpoints.
React UI Service

Layer / File(s)	Summary
UI build setup and auth service layer
ui-service/build.gradle, ui-service/package.json, ui-service/public/index.html, ui-service/.gitignore, ui-service/src/services/authService.js, ui-service/src/services/axiosInstance.js	Gradle Node plugin wires buildReactApp/startReactApp into jar/build/bootRun; authService performs OAuth2 password grant, stores tokens in localStorage, exposes isAuthenticated/decodeToken/getUserRoles; axiosInstance attaches bearer token on requests and redirects to /login on 401 responses.
UI components, routing, and styles
ui-service/src/index.js, ui-service/src/App.js, ui-service/src/components/ProtectedRoute.js, ui-service/src/components/Login.js, ui-service/src/components/Dashboard.js, ui-service/src/components/StoragesTable.js, ui-service/src/index.css	App gates routes via isAuthenticated(); ProtectedRoute redirects unauthenticated users; Login form with loading/error state; Dashboard shows decoded username, roles, logout, and embeds StoragesTable; StoragesTable fetches storages with role-gated add/delete admin actions; full CSS stylesheet.
Observability: Logback, Dashboards, and Dev Tooling

Layer / File(s)	Summary
Cross-service logback structured logging
config-service/build.gradle, config-service/src/main/resources/logback-spring.xml, discovery-service/build.gradle, discovery-service/src/main/resources/logback-spring.xml, gateway/src/main/resources/logback-spring.xml, resource-processor/src/main/resources/logback-spring.xml, resource-service/src/main/resources/logback-spring.xml, song-service/src/main/resources/logback-spring.xml, storage-service/src/main/resources/logback-spring.xml	logstash-logback-encoder:7.4 added to config-service and discovery-service; traceId/spanId MDC fields added to CONSOLE encoder patterns; LOGSTASH JSON appender added to root logger across all services; UTC timezone specified in JSON timestamp providers.
Grafana dashboards, Postman collection, and dev test scripts
tools/dashboards/gateway-metrics.json, tools/dashboards/jvm-metrics.json, tools/api-tests/module8-security-tests.json, feedback.md, run_test.bat, run_test.cmd, run_test.sh, run_tests.py	API Gateway Performance dashboard (request rate, 5xx rate, p50/p95/p99 latency, stat tiles); JVM Metrics dashboard (heap, non-heap, threads, GC, CPU, uptime); Postman collection for OAuth2 token and storage authorization tests; feedback.md documenting auth test failure analysis; Windows/Python scripts for running auth-service tests locally.

tools/dashboards/jvm-metrics.json-22-31 (1)
22-31: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Add job filter to JVM metric queries.

The job template variable is defined (lines 201-216) but is not referenced in any panel queries. This means the "Service" dropdown will have no effect—all services' metrics will always be displayed regardless of selection.

🔧 Proposed fix to add job filtering
Apply the job filter to all metric queries. For example, for the heap memory panel:

       "targets": [
         {
-          "expr": "jvm_memory_used_bytes{area=\"heap\"}",
+          "expr": "jvm_memory_used_bytes{area=\"heap\", job=~\"$job\"}",
           "legendFormat": "{{job}} - {{id}}",
Apply the same pattern to all other panels:

Line 53: jvm_memory_used_bytes{area=\"nonheap\", job=~\"$job\"}
Line 82: jvm_threads_live_threads{job=~\"$job\"}
Line 90: jvm_threads_daemon_threads{job=~\"$job\"}
Line 116: rate(jvm_gc_pause_seconds_sum{job=~\"$job\"}[1m]) / rate(jvm_gc_pause_seconds_count{job=~\"$job\"}[1m])
Line 142: process_cpu_usage{job=~\"$job\"}
Line 150: system_cpu_usage{job=~\"$job\"}
Line 178: process_uptime_seconds{job=~\"$job\"}
Use job=~\"$job\" to support the multi-select and "All" options.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@tools/dashboards/jvm-metrics.json` around lines 22 - 31, The job template
variable is defined but not referenced in any of the panel metric queries,
making the Service dropdown ineffective. Add the job filter to all metric
expressions in the dashboard by appending job=~"$job" to each query's label
matcher. Update the expressions for jvm_memory_used_bytes (heap at line 22),
jvm_memory_used_bytes (nonheap at line 53), jvm_threads_live_threads (line 82),
jvm_threads_daemon_threads (line 90), jvm_gc_pause_seconds_sum and
jvm_gc_pause_seconds_count (line 116), process_cpu_usage (line 142),
system_cpu_usage (line 150), and process_uptime_seconds (line 178) to include
the job filter using the pattern job=~"$job" within their label matcher brackets
to support multi-select functionality.

auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java-99-109 (1)
99-109: ⚠️ Potential issue | 🟠 Major | 🏗️ Heavy lift

Persist signing keys instead of generating ephemeral RSA keys on startup.

Lines 99-121 create a fresh key pair every process start. That invalidates previously issued tokens after restart and can break validation in multi-instance deployments when instances expose different JWKs.

Use a shared/persistent key source (keystore, KMS/HSM, or externally mounted key material) with rotation strategy.

Also applies to: 111-121

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java` around
lines 99 - 109, The jwkSource() method currently generates a new RSA key pair on
every startup via generateRsaKey(), which creates ephemeral keys that invalidate
previously issued tokens and cause inconsistency across multi-instance
deployments. Replace the ephemeral key generation with a persistent key source
by either loading keys from a keystore file, retrieving them from a KMS/HSM
service, or reading externally mounted key material. Implement a key rotation
strategy that reuses the same key material across restarts while allowing for
periodic key rotation without breaking existing token validation.
auth-service/src/main/java/com/audio/auth/config/DataInitializer.java-18-31 (1)
18-31: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Avoid unconditional seeding with predictable credentials.

Line 18 runs seeding whenever the table is empty, and Lines 21/28 set easily guessable defaults. This can introduce a valid default credential path in non-local deployments.

Gate this initializer behind a local/dev profile (or explicit auth.seed.enabled=true) and source seed passwords from environment/config secrets instead of literals.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/java/com/audio/auth/config/DataInitializer.java` around
lines 18 - 31, The DataInitializer class unconditionally seeds the database with
hardcoded credentials when the user table is empty, which creates a security
vulnerability. To fix this, add a Spring profile condition (such as
`@Profile`("dev") or `@Profile`("local")) to the DataInitializer class so seeding
only occurs in development environments, or alternatively add a configuration
property check (such as auth.seed.enabled) before executing the seeding logic.
Additionally, replace the hardcoded password literals in lines 21 and 28 where
passwordEncoder.encode("alice") and passwordEncoder.encode("bob") are called
with passwords read from environment variables or configuration properties
(using `@Value` or Environment injection) to externalize the seed credentials.
auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java-77-77 (1)
77-77: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Do not keep client secret in {noop} plaintext form.

Line 77 stores the OAuth client secret without hashing, which weakens client credential handling and increases exposure risk.

Suggested fix
-    public RegisteredClientRepository registeredClientRepository() {
+    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
         RegisteredClient oidcClient = RegisteredClient.withId(UUID.randomUUID().toString())
                 .clientId("gateway")
-                .clientSecret("{noop}gateway-secret")
+                .clientSecret(passwordEncoder.encode("gateway-secret"))
                 .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java` at line
77, The clientSecret method in SecurityConfig is storing the OAuth client secret
in plaintext using the {noop} prefix, which disables password encoding and
creates a security vulnerability. Remove the {noop} prefix and instead apply
proper password encoding such as bcrypt (using {bcrypt} prefix) or configure a
PasswordEncoder bean to hash the client secret securely before storage.
auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java-81-81 (1)
81-81: ⚠️ Potential issue | 🟠 Major

Password grant type is registered but not implemented—token endpoint will return unsupported_grant_type error.

Line 81 registers AuthorizationGrantType("password"), but Spring Authorization Server requires custom wiring to support it. The code lacks:

Custom OAuth2AuthenticationConverter to extract username/password from token requests
Custom OAuth2AuthenticationProvider to validate credentials
OAuth2TokenEndpointConfigurer configuration to integrate these at the token endpoint
The DaoAuthenticationProvider (lines 155–168) handles traditional authentication, not OAuth2 password grant. Implement custom grant type extension per Spring Authorization Server guides.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java` at line
81, The password grant type registered at line 81 with
authorizationGrantType(new AuthorizationGrantType("password")) in SecurityConfig
lacks the necessary implementation components. You need to create a custom
OAuth2AuthenticationConverter to parse username and password from token
requests, implement a custom OAuth2AuthenticationProvider to validate those
credentials using the existing DaoAuthenticationProvider (lines 155–168), and
configure these components in the OAuth2TokenEndpointConfigurer to handle the
password grant type at the token endpoint. Ensure the custom converter and
provider are properly wired into the authorization server configuration to
enable the password grant flow.
auth-service/src/test/java/com/audio/auth/AuthenticationTest.java-116-214 (1)
116-214: ⚠️ Potential issue | 🟠 Major | 🏗️ Heavy lift

Core token issuance/claim tests are disabled in a security-focused change.

Most happy-path token tests are @Disabled, so regressions in token issuance and JWT claim mapping won’t be caught in CI. At least one end-to-end success path should be enabled before merge.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/test/java/com/audio/auth/AuthenticationTest.java` around
lines 116 - 214, Remove the `@Disabled` annotation from at least one of the
happy-path token tests to ensure core token issuance and JWT claim validation
are verified in CI. Consider re-enabling testObtainAccessTokenWithAdminRole(),
testAccessTokenContainsRoles(), and
testAccessTokenWithUserRoleContainsUserAuthority() since these verify critical
functionality like token creation and role claims mapping. Keep
testInvalidUserCredentials enabled to validate error handling as well.
auth-service/Dockerfile-12-17 (1)
12-17: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Run the runtime container as a non-root user.

The final image has no USER directive, so the app runs as root. Drop privileges in the runtime stage.

🐳 Suggested hardening
 FROM eclipse-temurin:21-jre-alpine
-RUN apk add --no-cache curl
+RUN apk add --no-cache curl \
+    && addgroup -S app && adduser -S app -G app
 WORKDIR /app
 COPY --from=builder /app/auth-service/build/libs/*.jar app.jar
+RUN chown app:app /app/app.jar
+USER app
 EXPOSE 9000
 CMD ["java", "-jar", "app.jar"]
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/Dockerfile` around lines 12 - 17, Add a USER directive to the
runtime container stage to run the application as a non-root user instead of
root. Before the CMD instruction that runs java -jar app.jar, create a new
non-root user (such as appuser) using RUN apk commands with appropriate
permissions, and then add a USER directive to specify that this user should
execute the application. This applies to the final stage after the EXPOSE 9000
line and before the CMD instruction.
Source: Linters/SAST tools

auth-service/src/main/resources/application-docker.yml-10-12 (1)
10-12: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Avoid committed static DB credentials in the Docker profile.

username/password are hardcoded (postgres/postgres). This weakens security and leaks secrets through repo history. Externalize them via environment variables/secrets.

🔐 Suggested change
   datasource:
     url: jdbc:postgresql://auth-db:5432/auth_db
-    username: postgres
-    password: postgres
+    username: ${AUTH_DB_USERNAME}
+    password: ${AUTH_DB_PASSWORD}
     driver-class-name: org.postgresql.Driver
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/resources/application-docker.yml` around lines 10 - 12,
The hardcoded database credentials (username and password both set to
"postgres") in the application-docker.yml file present a security risk by
exposing sensitive information in version control. Replace the hardcoded values
for the username and password properties with environment variable references
using Spring's property placeholder syntax (e.g., ${SPRING_DATASOURCE_USERNAME}
and ${SPRING_DATASOURCE_PASSWORD}), then ensure these environment variables are
properly injected at runtime through Docker environment configuration or a
secrets management system.
ui-service/src/services/authService.js-7-11 (1)
7-11: ⚠️ Potential issue | 🟠 Major | 🏗️ Heavy lift

Do not ship OAuth client secrets in SPA code.

Line 11 embeds client_secret in browser-delivered JS, so it is publicly recoverable and cannot be treated as confidential. Move browser auth to Authorization Code + PKCE (public client) or a backend/BFF token exchange, and remove secret handling from the frontend.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/services/authService.js` around lines 7 - 11, The hardcoded
client_secret value in the params.append call within the authService.js
authentication flow exposes confidential credentials in browser-delivered
JavaScript. Remove the line that appends the client_secret (the
params.append('client_secret', 'gateway-secret') call) and refactor the
authentication mechanism to use Authorization Code with PKCE flow for public
clients, or implement a backend/BFF token exchange endpoint that securely
handles client credentials server-side instead of exposing them in frontend
code. This ensures the client secret remains confidential and never reaches the
browser.
ui-service/src/services/axiosInstance.js-4-6 (1)
4-6: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Remove fixed localhost API base URL.

Line 5 hardcodes the API origin and will fail outside local dev. Use a relative base URL or environment variable so the same bundle works across environments.

💡 Suggested change
 const axiosInstance = axios.create({
-  baseURL: 'http://localhost:8080'
+  baseURL: process.env.REACT_APP_API_BASE_URL || ''
 });
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/services/axiosInstance.js` around lines 4 - 6, The
axiosInstance object hardcodes the baseURL to 'http://localhost:8080', which
will fail in non-local environments. Replace the hardcoded localhost URL with
either a relative base URL (such as an empty string or relative path) or an
environment variable that can be configured per environment. Update the baseURL
property in the axios.create() call to use process.env or a similar environment
configuration mechanism so the same bundle can work across development, staging,
and production environments.
ui-service/src/services/authService.js-3-3 (1)
3-3: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Externalize auth base URL instead of hardcoding localhost.

Line 3 hardcodes http://localhost:9000, which breaks Docker/prod deployments and ties auth to a developer machine origin. Use env-driven or relative routing.

💡 Suggested change
-const AUTH_URL = 'http://localhost:9000/auth';
+const AUTH_URL = process.env.REACT_APP_AUTH_BASE_URL || '/auth';
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/services/authService.js` at line 3, The AUTH_URL constant on
line 3 is hardcoded to http://localhost:9000/auth which causes deployment
failures in Docker and production environments. Replace this hardcoded value
with an environment variable reference (such as process.env.AUTH_BASE_URL or
process.env.AUTH_URL) that can be configured per environment, and ensure a
sensible default fallback is provided if the environment variable is not set.
This allows the auth service URL to be dynamically configured based on the
deployment environment rather than tied to a developer's local machine.
gateway/src/main/java/com/audio/gateway/config/SecurityConfig.java-24-25 (1)
24-25: ⚠️ Potential issue | 🟠 Major

Narrow unauthenticated actuator access at the gateway

Line 24 permits all /actuator/** endpoints without authentication. While the application.yaml currently exposes only health, info, and refresh endpoints, the security configuration lacks defense-in-depth. If management.endpoints.web.exposure.include is expanded, any newly exposed endpoints automatically become publicly accessible. Configure authentication to require it for sensitive actuator operations.

Suggested hardening
-                        .pathMatchers("/actuator/**").permitAll()
+                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
+                        .pathMatchers("/actuator/**").authenticated()
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@gateway/src/main/java/com/audio/gateway/config/SecurityConfig.java` around
lines 24 - 25, The SecurityConfig class uses
pathMatchers("/actuator/**").permitAll() which permits all actuator endpoints
without authentication, creating a security risk if management endpoints are
expanded. Replace this overly permissive rule with more granular pathMatchers
that explicitly allow only safe, non-sensitive endpoints like /actuator/health
and /actuator/info without authentication, while requiring authentication for
all other actuator endpoints through additional pathMatchers rules. This ensures
new actuator endpoints added to the exposure configuration will not
automatically become publicly accessible and provides defense-in-depth security
regardless of future configuration changes.
storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java-23-25 (1)
23-25: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Missing JWT authority converter will break role-based authorization.

Same issue as song-service: Customizer.withDefaults() doesn't map the auth-server's roles claim to ROLE_* authorities. Any @PreAuthorize("hasRole('...')") annotations in StorageController will fail at runtime.

See the proposed fix in the song-service SecurityConfig.java review comment for the required JwtAuthenticationConverter bean.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java`
around lines 23 - 25, The oauth2ResourceServer JWT configuration in
SecurityConfig is using Customizer.withDefaults() which does not map the
auth-server's roles claim to Spring Security's ROLE_* authorities, causing
`@PreAuthorize`("hasRole('...')") annotations in StorageController to fail. Create
a JwtAuthenticationConverter bean in SecurityConfig that extracts the roles
claim from the JWT token and converts each role to ROLE_* format using a custom
GrantedAuthoritiesConverter, then configure the jwt() method to use this
converter instead of Customizer.withDefaults().
song-service/src/main/java/com/audio/song/config/SecurityConfig.java-29-31 (1)
29-31: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Missing JWT authority converter will break role-based authorization.

The gateway configures a custom JwtAuthenticationConverter that maps the roles claim to ROLE_* authorities (see gateway/src/main/java/com/audio/gateway/config/SecurityConfig.java:34-41). However, this service uses Customizer.withDefaults(), which reads from scope/scp claims with a SCOPE_ prefix by default.

This means @PreAuthorize("hasRole('ADMIN')") checks in SongController will fail at runtime because the roles claim from the auth-server won't be converted to ROLE_ADMIN authorities.

🔧 Proposed fix: Add a custom JWT authority converter
 package com.audio.song.config;
 
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.security.config.Customizer;
 import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
+import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
+import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
 import org.springframework.security.web.SecurityFilterChain;
 import org.springframework.web.cors.CorsConfiguration;
 import org.springframework.web.cors.CorsConfigurationSource;
 import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
 
 import java.util.Arrays;
 import java.util.List;
 
 `@Configuration`
 `@EnableWebSecurity`
 `@EnableMethodSecurity`
 public class SecurityConfig {
 
     `@Bean`
     public SecurityFilterChain securityFilterChain(HttpSecurity http) {
         http
             .authorizeHttpRequests(auth -> auth
                 .anyRequest().authenticated()
             )
             .oauth2ResourceServer(oauth2 -> oauth2
-                .jwt(Customizer.withDefaults())
+                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
             )
             .cors(Customizer.withDefaults())
             .csrf(AbstractHttpConfigurer::disable);
 
         return http.build();
     }
+
+    `@Bean`
+    public JwtAuthenticationConverter jwtAuthenticationConverter() {
+        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
+        authoritiesConverter.setAuthorityPrefix("ROLE_");
+        authoritiesConverter.setAuthoritiesClaimName("roles");
+
+        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
+        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
+        return converter;
+    }
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@song-service/src/main/java/com/audio/song/config/SecurityConfig.java` around
lines 29 - 31, The SecurityConfig class is using Customizer.withDefaults() for
JWT configuration which reads from scope claims with SCOPE_ prefix, but the
authorization checks in SongController expect ROLE_* authorities derived from a
roles claim. Replace the Customizer.withDefaults() in the oauth2ResourceServer
jwt configuration with a custom JwtAuthenticationConverter bean that maps the
roles claim to ROLE_* authorities, similar to the implementation found in the
gateway SecurityConfig. This converter should extract the roles from the JWT
claims and convert each role to an authority with the ROLE_ prefix to align with
the `@PreAuthorize` checks.
storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java-17-30 (1)
17-30: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Missing CORS configuration for React UI access.

Unlike song-service, this config lacks a CorsConfigurationSource bean. With only .cors(Customizer.withDefaults()), Spring will not allow cross-origin requests from http://localhost:3000 (the React UI). This will cause CORS errors when the UI attempts to manage storages.

🔧 Proposed fix: Add CorsConfigurationSource bean
 package com.audio.storage.config;
 
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.security.config.Customizer;
 import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
 import org.springframework.security.config.annotation.web.builders.HttpSecurity;
 import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
 import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
 import org.springframework.security.web.SecurityFilterChain;
+import org.springframework.web.cors.CorsConfiguration;
+import org.springframework.web.cors.CorsConfigurationSource;
+import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
+
+import java.util.Arrays;
+import java.util.List;
 
 `@Configuration`
 `@EnableWebSecurity`
 `@EnableMethodSecurity`
 public class SecurityConfig {
 
     `@Bean`
     public SecurityFilterChain securityFilterChain(HttpSecurity http) {
         // ... existing code ...
     }
+
+    `@Bean`
+    public CorsConfigurationSource corsConfigurationSource() {
+        CorsConfiguration configuration = new CorsConfiguration();
+        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
+        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
+        configuration.setAllowedHeaders(List.of("*"));
+        configuration.setAllowCredentials(true);
+        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
+        source.registerCorsConfiguration("/**", configuration);
+        return source;
+    }
 }
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@storage-service/src/main/java/com/audio/storage/config/SecurityConfig.java`
around lines 17 - 30, The SecurityConfig class's securityFilterChain method uses
`.cors(Customizer.withDefaults())` which does not properly configure CORS for
cross-origin requests from the React UI at http://localhost:3000. Create a new
CorsConfigurationSource bean that explicitly configures allowed origins
(http://localhost:3000), allowed HTTP methods (GET, POST, PUT, DELETE, etc.),
and allowed headers. Then update the securityFilterChain method to use this bean
instead of the default customizer by passing it to the cors configuration.
.env-60-60 (1)
60-60: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

CONFIG_REPO_URI default points to a mutable external Git source.

Using a shared .env default that targets an external mutable repo can introduce config drift/supply-chain risk for security settings.

Consider defaulting to an org-controlled repo (or immutable ref strategy) for shared environments.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In @.env at line 60, The CONFIG_REPO_URI environment variable is currently
pointing to an external mutable Git repository (PashaPoliak/config-repo.git),
which introduces supply-chain and config drift risks. Replace the default value
of CONFIG_REPO_URI in the .env file with a reference to an
organization-controlled repository instead of relying on an external mutable
source, or implement an immutable reference strategy (such as pinning to a
specific commit hash) to ensure configuration integrity and security for shared
environments.
config-repo/auth-service-docker.yml-9-13 (1)
9-13: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

Avoid hardcoded auth DB credentials and org.springframework.security: DEBUG in docker defaults.

This profile is used by the compose auth-service, so these defaults directly affect runtime security posture.

Suggested hardening
 spring:
   datasource:
     url: jdbc:postgresql://auth-db:5432/auth_db
-    username: postgres
-    password: postgres
+    username: ${POSTGRES_AUTH_USER}
+    password: ${POSTGRES_AUTH_PASSWORD}
@@
 logging:
   level:
     root: INFO
-    org.springframework.security: DEBUG
+    org.springframework.security: INFO
Also applies to: 24-25

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@config-repo/auth-service-docker.yml` around lines 9 - 13, The datasource
configuration contains hardcoded PostgreSQL credentials (username and password
fields set to "postgres") and likely has DEBUG logging enabled for
org.springframework.security (referenced at lines 24-25), both of which
compromise security in a production Docker environment. Remove the hardcoded
username and password values from the datasource section and replace them with
environment variable placeholders or use Spring's externalized configuration
approach. Additionally, change the logging level for
org.springframework.security from DEBUG to a less verbose level (like INFO or
WARN) to avoid exposing sensitive information in logs.
storage-service/src/main/java/com/audio/storage/controller/StorageController.java-30-33 (1)
30-33: ⚠️ Potential issue | 🟠 Major | ⚡ Quick win

POST /storages currently returns 200, not the expected 201.

The downstream security test collection expects admin create to return 201, but ResponseEntity.ok(...) returns 200.

Suggested fix
+import org.springframework.http.HttpStatus;
@@
     `@PostMapping`
     `@PreAuthorize`("hasRole('ADMIN')")
     public ResponseEntity<StorageCreateResponse> create(`@Valid` `@RequestBody` StorageCreateRequest request) {
-        return ResponseEntity.ok(storageService.create(request));
+        return ResponseEntity.status(HttpStatus.CREATED).body(storageService.create(request));
     }
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In
`@storage-service/src/main/java/com/audio/storage/controller/StorageController.java`
around lines 30 - 33, The create method in StorageController is returning HTTP
200 (OK) instead of the expected HTTP 201 (Created) for POST resource creation.
Change the ResponseEntity.ok() call to use ResponseEntity.created() or
ResponseEntity.status(HttpStatus.CREATED) to return the correct HTTP 201 status
code when a storage resource is successfully created.
storage-service/src/main/java/com/audio/storage/controller/StorageController.java-31-44 (1)
31-44: ⚠️ Potential issue | 🟠 Major

Storage service uses default JWT converter that ignores custom roles claim.

The @PreAuthorize("hasRole('ADMIN')") guards expect ROLE_ADMIN authority, but storage-service's SecurityConfig (line 23-24) only uses .jwt(Customizer.withDefaults()), which maps only standard scope/scp claims. The auth-service emits a custom roles claim, but without explicit configuration via JwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles") and .setAuthorityPrefix("ROLE_"), this claim is ignored during token validation. Valid admin tokens will receive 403 Forbidden.

Configure a custom JWT authentication converter in storage-service to map the roles claim to Spring Security authorities.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In
`@storage-service/src/main/java/com/audio/storage/controller/StorageController.java`
around lines 31 - 44, The SecurityConfig class currently uses default JWT
configuration that only recognizes standard scope claims, but needs to extract
the custom roles claim from auth-service tokens. Update the SecurityConfig
(specifically where .jwt(Customizer.withDefaults()) is configured) to use a
custom JwtGrantedAuthoritiesConverter that maps the roles claim to Spring
Security authorities. Configure the converter by setting the
authoritiesClaimName to roles and the authorityPrefix to ROLE_ so that the
`@PreAuthorize` guards in StorageController can properly recognize admin users.
🟡 Minor comments (7)
auth-service/src/test/java/com/audio/auth/AuthenticationTest.java-41-42 (1)
41-42: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

JWT roles claim expectations don’t match the auth-service claim contract.

Assertions expect ROLE_ADMIN / ROLE_USER, but the token customizer emits role values without the ROLE_ prefix. These checks will fail once enabled.

✅ Suggested fix
-    private static final String ROLE_ADMIN = "ROLE_ADMIN";
-    private static final String ROLE_USER = "ROLE_USER";
+    private static final String ROLE_ADMIN = "ADMIN";
+    private static final String ROLE_USER = "USER";
Also applies to: 172-175, 197-199

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/test/java/com/audio/auth/AuthenticationTest.java` around
lines 41 - 42, The test constants ROLE_ADMIN and ROLE_USER include the "ROLE_"
prefix, but the token customizer emits role claim values without this prefix,
causing assertion mismatches. Update the constant definitions for ROLE_ADMIN and
ROLE_USER to remove the "ROLE_" prefix (so they become "ADMIN" and "USER"
respectively), and ensure all assertions that use these constants throughout the
test file (including the locations at lines 172-175 and 197-199) are updated to
expect the role values without the prefix to match the actual token customizer
output.
ui-service/src/components/StoragesTable.js-34-44 (1)
34-44: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

Use functional state updates for async add/delete mutations.

Lines 34 and 44 use closure-captured storages; concurrent request completions can overwrite newer state and show stale rows. Use functional updates to always derive from latest state.

💡 Suggested change
-      setStorages([...storages, response.data]);
+      setStorages(prev => [...prev, response.data]);
@@
-      setStorages(storages.filter(s => s.id !== id));
+      setStorages(prev => prev.filter(s => s.id !== id));
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/components/StoragesTable.js` around lines 34 - 44, The
setStorages calls in both the add storage mutation handler (around line 34) and
the delete storage handler (around line 44) are using closure-captured storages
variable, which can cause race conditions when multiple requests complete
concurrently. Refactor both setStorages calls to use functional updates instead:
replace setStorages([...storages, response.data]) with setStorages that takes a
prevStorages parameter and returns the updated array, and similarly for the
filter operation in handleDeleteStorage. This ensures each state update always
derives from the latest state rather than a potentially stale closure value.
ui-service/src/index.css-3-4 (1)
3-4: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

Fix Stylelint font-family-name-quotes violations in the font stack.

Lines 3-4 quote single-word family names (Roboto, Oxygen, Ubuntu, Cantarell), which fails the configured lint rule.

💡 Suggested fix
 body {
   margin: 0;
-  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
-    'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
+  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen,
+    Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
   background-color: `#f5f5f5`;
 }
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/index.css` around lines 3 - 4, The font-family property in the
CSS file is violating the stylelint font-family-name-quotes rule by quoting
single-word font family names. Remove the single quotes around the single-word
font family names (Roboto, Oxygen, Ubuntu, and Cantarell) in the font-family
declaration while keeping the quotes around multi-word font family names (Segoe
UI, Fira Sans, Droid Sans, Helvetica Neue). Only quote font family names that
contain spaces.
Source: Linters/SAST tools

ui-service/build.gradle-19-21 (1)
19-21: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

Track public/ as an input for deterministic frontend builds.

Line 19 only tracks src; changes in public/index.html won’t invalidate buildReactApp, so stale static assets can be packaged.

💡 Suggested fix
 tasks.register('buildReactApp', NpmTask) {
     dependsOn 'npmInstall'
     args = ['run', 'build']

     inputs.dir("src")
+    inputs.dir("public")
     inputs.file("package.json")
     inputs.file("package-lock.json")
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/build.gradle` around lines 19 - 21, The buildReactApp task in the
build.gradle file is not tracking the public directory as an input, which means
changes to static assets like public/index.html won't invalidate the build cache
and stale assets can be packaged. Add inputs.dir("public") to the list of input
declarations alongside the existing inputs.dir("src"),
inputs.file("package.json"), and inputs.file("package-lock.json") to ensure the
build task is properly invalidated when files in the public directory change.
ui-service/src/App.js-10-14 (1)
10-14: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

Don’t render the login form for already-authenticated users on /login.

Line 10 short-circuits on path === '/login', so authenticated users still see the login screen.

💡 Suggested fix
 function App() {
   const path = window.location.pathname;
+  const authenticated = isAuthenticated();

-  if (path === '/login' || !isAuthenticated()) {
+  if (path === '/login' && !authenticated) {
     return (
       <Login onLoginSuccess={() => { window.location.href = '/dashboard'; }} />
     );
   }
+
+  if (!authenticated) {
+    return (
+      <Login onLoginSuccess={() => { window.location.href = '/dashboard'; }} />
+    );
+  }
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/src/App.js` around lines 10 - 14, The condition in the App.js file
that checks `if (path === '/login' || !isAuthenticated())` is causing
authenticated users to see the login form when they visit the `/login` route.
Remove the `path === '/login'` check from this condition so that the login form
is only rendered when the user is not authenticated. Additionally, add a
separate check before this condition to redirect already-authenticated users who
try to access the `/login` path to the dashboard (or appropriate authenticated
route) using window.location.href or a redirect mechanism.
song-service/src/test/java/com/audio/song/SongServiceSecurityTest.java-50-55 (1)
50-55: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

GET tests may fail or behave unexpectedly without mocking songService.getSong().

The userRoleCanGetSong and adminRoleCanGetSong tests don't mock songService.getSong(). When the controller invokes the mocked service, it will return null, which may cause the test to pass for the wrong reason (controller returning null wrapped in 200 OK) or fail if the service throws.

🧪 Proposed fix
+    `@Test`
+    void userRoleCanGetSong() throws Exception {
+        when(songService.getSong(1L)).thenReturn(new SongRequest(...));
+
         mockMvc.perform(get("/songs/1")
                         .with(jwt().authorities(() -> "ROLE_USER")))
                 .andExpect(status().isOk());
     }
Add similar mock setup for adminRoleCanGetSong().

Also applies to: 73-78

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@song-service/src/test/java/com/audio/song/SongServiceSecurityTest.java`
around lines 50 - 55, The userRoleCanGetSong and adminRoleCanGetSong test
methods lack mock setup for the songService.getSong() method, which can cause
tests to pass for incorrect reasons (controller returning null) or fail
unexpectedly. Add a mock setup before each test's mockMvc.perform() call to mock
songService.getSong() with a Song ID of 1 and return a valid Song object,
ensuring the controller receives expected data and the test validates the actual
security behavior rather than null handling.
config-repo/auth-service.yml-24-25 (1)
24-25: ⚠️ Potential issue | 🟡 Minor | ⚡ Quick win

DEBUG logging for security may leak sensitive data in non-development environments.

org.springframework.security: DEBUG logs detailed authentication flows including tokens and credential validation. Consider using INFO level by default and enabling DEBUG only in development profiles to prevent accidental exposure.

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@config-repo/auth-service.yml` around lines 24 - 25, The
org.springframework.security logger is set to DEBUG level which logs sensitive
authentication data like tokens and credentials. Change the logging level for
org.springframework.security from DEBUG to INFO in the default configuration,
then create a separate development profile (or use an existing dev/development
profile) where you can explicitly set org.springframework.security to DEBUG.
This ensures sensitive logging only occurs in development environments and not
in production deployments.
ℹ️ Review info
⚙️ Run configuration
📥 Commits
⛔ Files ignored due to path filters (1)
📒 Files selected for processing (80)
💤 Files with no reviewable changes (1)
resource-service/src/main/java/com/audio/resource/messaging/ResourceEventConsumer.java
auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java
Comment on lines +54 to +56
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/auth/oauth2/**").permitAll()
                .anyRequest().authenticated()


With server.servlet.context-path=/auth, should AuthorizationServerSettings.tokenEndpoint(...)be configured as/oauth2/tokenor/auth/oauth2/token in Spring Authorization Server? How are these paths combined?

💡 Result:

When configuring Spring Authorization Server with server.servlet.context-path=/auth, you should continue to use the default setting of /oauth2/token for AuthorizationServerSettings.tokenEndpoint(...) [1][2]. You do not need to include the context path in the endpoint setting [3]. Spring MVC and the underlying Servlet container handle the context path automatically [3]. When you set server.servlet.context-path=/auth, the application effectively treats /auth as the base of your application's request mapping [3]. Consequently, a request to /auth/oauth2/token is correctly routed to the token endpoint mapped at /oauth2/token because the servlet container strips the context path before the request reaches the Spring DispatcherServlet. These paths are combined at the servlet container level rather than via manual concatenation in the AuthorizationServerSettings configuration [3]. Setting the endpoint to /auth/oauth2/token within AuthorizationServerSettings would result in an incorrect mapping (e.g., resulting in an effective path of /auth/auth/oauth2/token in some configurations), which would lead to 404 errors for your OAuth 2.0 endpoints [3]. For consistent behavior, you should let the issuer identifier resolve dynamically or, if you must explicitly set it, ensure it reflects the full public base URL (e.g., https://example.com/auth) without adding additional path segments that conflict with the servlet context path [3]. Keep in mind that Spring Authorization Server's AuthorizationServerContext resolves endpoint paths relative to the application's runtime context [3][4].


find . -name "SecurityConfig.java" -type f

wc -l ./auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java
Repository: Lokankara/Microservices

Length of output: 142

🏁 Script executed:

cat -n ./auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java
Repository: Lokankara/Microservices

Length of output: 9299

🏁 Script executed:

find . -name "application.properties" -o -name "application.yml" -o -name "application.yaml" | head -20
Repository: Lokankara/Microservices

Length of output: 680

cat ./auth-service/src/main/resources/application.yml

Length of output: 558

Fix double-prefixed OAuth2 endpoint paths.

With server.servlet.context-path: /auth, configure AuthorizationServerSettings endpoints as /oauth2/authorize and /oauth2/token (without the /auth prefix). Similarly, update the security matcher to /oauth2/**. The servlet container automatically prepends the context path; including it explicitly results in /auth/auth/oauth2/token and breaks OAuth2 flows.

Required fixes:

Line 143: Change .authorizationEndpoint("/auth/oauth2/authorize") to .authorizationEndpoint("/oauth2/authorize")
Line 144: Change .tokenEndpoint("/auth/oauth2/token") to .tokenEndpoint("/oauth2/token")
Line 55: Change .requestMatchers("/auth/oauth2/**") to .requestMatchers("/oauth2/**")
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java` around
lines 54 - 56, The OAuth2 endpoint paths have double prefixes due to the
configured context path. Since server.servlet.context-path is set to /auth, the
servlet container automatically prepends /auth to all paths, making manual
inclusion of /auth redundant. Remove the /auth prefix from the
authorizationEndpoint method call and the tokenEndpoint method call so they
become /oauth2/authorize and /oauth2/token respectively. Additionally, update
the requestMatchers security configuration from /auth/oauth2/** to /oauth2/** to
match the corrected endpoint paths. This ensures OAuth2 flows work correctly
without resulting in double-prefixed paths like /auth/auth/oauth2/token.
@PashaPoliak	Reply...
config-repo/gateway.yml
Comment on lines +39 to +40
          issuer-uri: http://localhost:9000/auth
          jwk-set-uri: http://localhost:9000/auth/.well-known/jwks.json

1 hour ago
⚠️ Potential issue | 🔴 Critical

🧩 Analysis chain
🌐 Web query:

Spring Authorization Server default JWKS endpoint path

💡 Result:

The default path for the JWK Set (JWKS) endpoint in Spring Authorization Server is /oauth2/jwks [1][2][3]. This endpoint is configured via the AuthorizationServerSettings class [2][4]. It is important to note that the JWKS endpoint is only enabled and exposed if a JWKSource bean is registered in your application configuration [2][4]. You can customize this path using the jwkSetEndpoint method when building your AuthorizationServerSettings bean [2][4].

Citations:

1: https://www.springframework.org/spring-security/reference/api/java/org/springframework/security/oauth2/server/authorization/settings/AuthorizationServerSettings.html
2: https://docs.spring.io/spring-authorization-server/reference/configuration-model.html
3: https://docs.enterprise.spring.io/spring-authorization-server/docs/1.4.9/api/org/springframework/security/oauth2/server/authorization/settings/AuthorizationServerSettings.html
4: https://docs.enterprise.spring.io/spring-authorization-server/reference/configuration-model.html
# Search for Spring Authorization Server configuration or setup
rg -i "authorization.server|spring.*auth.*server|jwk.*source" --type yaml --type java --type kotlin -A 3

# Check for application properties related to OAuth/OIDC configuration
rg -i "spring.security.oauth2|issuer-uri|jwk-set-uri" --type yaml --type properties -B 2 -A 2

# Look for auth server related files or documentation
fd -i "auth\|oauth\|security" -t f | head -20


Length of output: 4763

# Check the auth-service SecurityConfig for AuthorizationServerSettings configuration
rg -A 20 "AuthorizationServerSettings" auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java

# Also check if there's any endpoint configuration mentioning jwks or oauth2
rg -i "endpoint|jwks|oauth2" auth-service/src/main/java/com/audio/auth/config/SecurityConfig.java -B 2 -A 2


Length of output: 5425

Incorrect issuer-uri and JWKS endpoint path will cause JWT validation failures.

The issuer configured in the auth server is http://localhost:9000, but the gateway uses http://localhost:9000/auth for the issuer-uri. Additionally, Spring Authorization Server exposes the JWK set at /oauth2/jwks, not /.well-known/jwks.json. Both mismatches must be corrected for JWT validation to work.

Proposed fix
       jwt:
-        issuer-uri: http://localhost:9000/auth
-        jwk-set-uri: http://localhost:9000/auth/.well-known/jwks.json
+        issuer-uri: http://localhost:9000
+        jwk-set-uri: http://localhost:9000/oauth2/jwks
📝 Committable suggestion
‼️ IMPORTANT
Carefully review the code before committing. Ensure that it accurately replaces the highlighted code, contains no missing lines, and has no issues with indentation. Thoroughly test & benchmark the code to ensure it meets the requirements.

Suggested change
          issuer-uri: http://localhost:9000/auth
          jwk-set-uri: http://localhost:9000/auth/.well-known/jwks.json
          issuer-uri: http://localhost:9000
          jwk-set-uri: http://localhost:9000/oauth2/jwks
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@config-repo/gateway.yml` around lines 39 - 40, The issuer-uri and jwk-set-uri
values in the Spring Security configuration are incorrectly configured and will
prevent JWT validation. Change the issuer-uri from `http://localhost:9000/auth`
to `http://localhost:9000` to match the actual issuer configured in the auth
server, and change the jwk-set-uri from
`http://localhost:9000/auth/.well-known/jwks.json` to
`http://localhost:9000/oauth2/jwks` since Spring Authorization Server exposes
the JWK set at the `/oauth2/jwks` endpoint path, not `/.well-known/jwks.json`.
@PashaPoliak	Reply...
config-repo/resource-service-docker.yml
Comment on lines +26 to +27
          issuer-uri: http://auth-service:9000/auth
          jwk-set-uri: http://auth-service:9000/auth/.well-known/jwks.json

1 hour ago
⚠️ Potential issue | 🔴 Critical | ⚡ Quick win

Incorrect JWKS endpoint path — same issue as gateway config.

The /auth/.well-known/jwks.json path does not exist on Spring Authorization Server. Use /auth/oauth2/jwks instead.

Proposed fix
         jwt:
           issuer-uri: http://auth-service:9000/auth
-          jwk-set-uri: http://auth-service:9000/auth/.well-known/jwks.json
+          jwk-set-uri: http://auth-service:9000/auth/oauth2/jwks
📝 Committable suggestion
‼️ IMPORTANT
Carefully review the code before committing. Ensure that it accurately replaces the highlighted code, contains no missing lines, and has no issues with indentation. Thoroughly test & benchmark the code to ensure it meets the requirements.

Suggested change
          issuer-uri: http://auth-service:9000/auth
          jwk-set-uri: http://auth-service:9000/auth/.well-known/jwks.json
          issuer-uri: http://auth-service:9000/auth
          jwk-set-uri: http://auth-service:9000/auth/oauth2/jwks
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@config-repo/resource-service-docker.yml` around lines 26 - 27, The
jwk-set-uri configuration in the resource-service-docker.yml file is using an
incorrect JWKS endpoint path. Spring Authorization Server does not have the
jwks.json file at the /auth/.well-known/ location. Update the jwk-set-uri value
by changing the endpoint path from /auth/.well-known/jwks.json to
/auth/oauth2/jwks to point to the correct JWKS endpoint on the auth-service.
@PashaPoliak	Reply...
config-repo/resource-service.yml
Comment on lines +33 to +34
          issuer-uri: http://localhost:9000/auth
          jwk-set-uri: http://localhost:9000/auth/.well-known/jwks.json

1 hour ago
⚠️ Potential issue | 🔴 Critical | ⚡ Quick win

Incorrect JWKS endpoint path — same issue as other configs.

Change /.well-known/jwks.json to /oauth2/jwks.

Proposed fix
📝 Committable suggestion
🤖 Prompt for AI Agents
@PashaPoliak	Reply...
config-repo/song-service-docker.yml
Comment on lines +13 to +14
          issuer-uri: http://auth-service:9000/auth
          jwk-set-uri: http://auth-service:9000/auth/.well-known/jwks.json

1 hour ago
⚠️ Potential issue | 🔴 Critical | ⚡ Quick win

Incorrect JWKS endpoint path — same issue as other configs.

Change /.well-known/jwks.json to /oauth2/jwks.

Proposed fix
📝 Committable suggestion
🤖 Prompt for AI Agents
@PashaPoliak	Reply...
2 hidden conversations
Load more…
config-repo/storage-service.yml
Comment on lines +11 to +12
          issuer-uri: http://localhost:9000/auth
          jwk-set-uri: http://localhost:9000/auth/.well-known/jwks.json

1 hour ago
⚠️ Potential issue | 🔴 Critical | ⚡ Quick win

Incorrect JWKS endpoint path — same issue as other configs.

Change /.well-known/jwks.json to /oauth2/jwks.

Proposed fix
📝 Committable suggestion
🤖 Prompt for AI Agents
@PashaPoliak	Reply...
resource-service/src/main/java/com/audio/resource/config/SecurityConfig.java
Comment on lines +24 to +26
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )

1 hour ago
⚠️ Potential issue | 🔴 Critical

🧩 Analysis chain
Configure JWT role-claim mapping to enable admin role enforcement

The resource-service uses the default JWT converter at lines 24–26, which only recognizes scope/scp claims. However, the auth server emits a roles claim without the ROLE_ prefix, and the controller enforces @PreAuthorize("hasRole('ADMIN')") at line 43. This mismatch causes admin role checks to fail against real tokens.

The gateway service already implements the correct pattern. Apply the same custom JWT authentication converter here:

Suggested fix
+import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
+import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
@@
     `@Bean`
     public SecurityFilterChain securityFilterChain(HttpSecurity http) {
+        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
+        authoritiesConverter.setAuthorityPrefix("ROLE_");
+        authoritiesConverter.setAuthoritiesClaimName("roles");
+
+        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
+        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
+
         http
@@
             .oauth2ResourceServer(oauth2 -> oauth2
-                .jwt(Customizer.withDefaults())
+                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
             )
📝 Committable suggestion
‼️ IMPORTANT
Carefully review the code before committing. Ensure that it accurately replaces the highlighted code, contains no missing lines, and has no issues with indentation. Thoroughly test & benchmark the code to ensure it meets the requirements.

Suggested change
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
`@Bean`
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix("ROLE_");
    authoritiesConverter.setAuthoritiesClaimName("roles");
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    http
        .authorizeHttpRequests(authz -> authz
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        )
        .build();
    
    return http.build();
}
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@resource-service/src/main/java/com/audio/resource/config/SecurityConfig.java`
around lines 24 - 26, Replace the default JWT converter at the
oauth2ResourceServer configuration in SecurityConfig with a custom JWT
authentication converter that handles role claim mapping. Create a custom
JwtAuthenticationConverter (or similar custom converter class) that extracts the
roles claim from the JWT token and maps it to Spring Security's expected format
by prefixing each role with ROLE_. Then configure the jwt method in
oauth2ResourceServer to use this custom converter instead of
Customizer.withDefaults(). Reference the gateway service's implementation
pattern for the correct approach to extract the roles claim and transform it
into GrantedAuthority objects that will satisfy the `@PreAuthorize` hasRole checks
on controllers like the one at line 43.
@PashaPoliak	Reply...
run_test.sh
tools/api-tests/module8-security-tests.json
Comment on lines +20 to +26
              "urlencoded": [
                { "key": "grant_type", "value": "password" },
                { "key": "username", "value": "alice" },
                { "key": "password", "value": "password123" },
                { "key": "client_id", "value": "gateway" },
                { "key": "client_secret", "value": "gateway-secret" }
              ]

1 hour ago
⚠️ Potential issue | 🔴 Critical | ⚡ Quick win

Incorrect password for alice user.

TC-9 uses password123 for the alice user, but the actual password is alice according to the test constants in AuthenticationTest.java:35.

🐛 Proposed fix
                 { "key": "grant_type", "value": "password" },
                 { "key": "username", "value": "alice" },
-                { "key": "password", "value": "password123" },
+                { "key": "password", "value": "alice" },
                 { "key": "client_id", "value": "gateway" },
🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@tools/api-tests/module8-security-tests.json` around lines 20 - 26, The
password value for the alice user in the urlencoded parameters is set to
"password123" but should be "alice" to match the test constants defined in
AuthenticationTest.java. Locate the password key in the urlencoded array for
TC-9 where the username is "alice" and change its value from "password123" to
"alice".
@PashaPoliak	Reply...
ui-service/build.gradle
Comment on lines +15 to +24
tasks.register('buildReactApp', NpmTask) {
    dependsOn 'npmInstall'
    args = ['run', 'build']

    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("package-lock.json")

    outputs.dir(layout.buildDirectory.dir("resources/main/static"))
}

1 hour ago
⚠️ Potential issue | 🔴 Critical

🧩 Analysis chain
#!/bin/bash
set -euo pipefail

echo "== ui-service/package.json build script =="
jq -r '.scripts.build // "<missing>"' ui-service/package.json

echo
echo "== Output-path wiring checks =="
rg -n "BUILD_PATH|resources/main/static|processResources|from\\(" ui-service/build.gradle ui-service/package.json


Length of output: 258

cat -n ui-service/build.gradle


Length of output: 1211

cat -n ui-service/package.json | head -30


Length of output: 958

rg -n "processResources|copy.*static|from.*build" ui-service/ --type gradle --type json


Length of output: 49

find . -maxdepth 2 -name "build.gradle" -o -name "settings.gradle" | head -10


Length of output: 334

cat -n ./build.gradle


Length of output: 713

rg -n "processResources|resources/main/static|from.*to" ui-service/ -A 3


Length of output: 558

# Check if BUILD_PATH or similar env var is set anywhere
rg -n "BUILD_PATH|GENERATE_SOURCEMAP" ui-service/


Length of output: 49

React build output is not wired into the Gradle package.

React's build script outputs to ./build/ by default (react-scripts standard behavior), but the buildReactApp task declares outputs at build/resources/main/static/ with no mechanism to move or copy files between them. Add either:

A BUILD_PATH=build/resources/main/static environment variable to the buildReactApp task, or
A Gradle copy task from build/ into src/main/resources/static/ before packaging.
Without this, the JAR will be missing UI assets.

Also applies to: 31-37

🤖 Prompt for AI Agents
Verify each finding against current code. Fix only still-valid issues, skip the
rest with a brief reason, keep changes minimal, and validate.

In `@ui-service/build.gradle` around lines 15 - 24, The buildReactApp task
declares outputs at build/resources/main/static/ but React's default build
script outputs to ./build/ instead, creating a mismatch that results in missing
UI assets in the final JAR. Fix this by adding an environment variable
BUILD_PATH=build/resources/main/static to the buildReactApp NpmTask definition
to redirect React's output to the expected location, or alternatively create a
separate Gradle copy task that moves files from the ./build/ directory to
src/main/resources/static/ after the React build completes. Either approach will
ensure the compiled React assets end up in the correct location for JAR
packaging.