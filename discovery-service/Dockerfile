# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build configs for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY discovery-service/build.gradle discovery-service/
RUN ./gradlew :discovery-service:dependencies --no-daemon

# Copy source code and build
COPY discovery-service/src discovery-service/src
RUN ./gradlew :discovery-service:assemble --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/discovery-service/build/libs/*.jar app.jar
EXPOSE 8761
CMD ["java", "-jar", "app.jar"]
