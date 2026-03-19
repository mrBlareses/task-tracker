# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle .

COPY cron-service ./cron-service

RUN chmod +x ./gradlew && ./gradlew --no-daemon :cron-service:clean :cron-service:bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/cron-service/build/libs/*.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]