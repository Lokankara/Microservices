# Stage 1: Build
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper and build configs for dependency caching
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY resource-processor/build.gradle resource-processor/
RUN ./gradlew :resource-processor:dependencies --no-daemon

# Copy source code and build
COPY resource-processor/src resource-processor/src
RUN ./gradlew :resource-processor:assemble --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/resource-processor/build/libs/*.jar app.jar
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
