# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# wrapper и settings из корня
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle .

# root-project собираем из backend-service
COPY backend-service/gradle.properties ./gradle.properties
COPY backend-service/build.gradle ./build.gradle
COPY backend-service/src ./src
COPY backend-service/openapi ./openapi

# обязательно копируем subproject, который объявлен в settings.gradle
COPY cron-service ./cron-service

# собираем root project (это и есть backend)
RUN chmod +x ./gradlew && ./gradlew --no-daemon clean bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]