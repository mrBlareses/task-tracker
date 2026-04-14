# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle .

COPY backend-service ./backend-service
COPY cron-service ./cron-service
COPY email-sender-service ./email-sender-service

RUN sed -i 's/\r$//' ./gradlew && \
    chmod +x ./gradlew && \
    ./gradlew --no-daemon :backend-service:clean :backend-service:bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/backend-service/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]