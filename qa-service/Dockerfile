# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build configs for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY qa-service/build.gradle qa-service/
RUN ./gradlew :qa-service:dependencies --no-daemon

# Copy source code and build
COPY qa-service/src qa-service/src
RUN ./gradlew :qa-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/qa-service/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
