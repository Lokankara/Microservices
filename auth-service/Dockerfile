FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY auth-service/build.gradle auth-service/
RUN ./gradlew :auth-service:dependencies --no-daemon

COPY auth-service/src auth-service/src
RUN ./gradlew :auth-service:assemble --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /app/auth-service/build/libs/*.jar app.jar
RUN chown app:app /app/app.jar
USER app
EXPOSE 9000
CMD ["java", "-jar", "app.jar"]
