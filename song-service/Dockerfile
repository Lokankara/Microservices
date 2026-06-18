# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build configs for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY song-service/build.gradle song-service/
RUN ./gradlew :song-service:dependencies --no-daemon

# Copy source code and build
COPY song-service/src song-service/src
RUN ./gradlew :song-service:assemble --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/song-service/build/libs/*.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
