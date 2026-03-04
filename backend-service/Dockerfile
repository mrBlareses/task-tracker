# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# копируем только то, что нужно для установки зависимостей
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./

# прогреваем зависимости
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies

# копируем исходники и openapi спеки
COPY src ./src
COPY openapi ./openapi

# собираем bootJar
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8081
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]