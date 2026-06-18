# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build configs for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gateway/build.gradle gateway/
RUN ./gradlew :gateway:dependencies --no-daemon

# Copy source code and build
COPY gateway/src gateway/src
RUN ./gradlew :gateway:assemble --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/gateway/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
