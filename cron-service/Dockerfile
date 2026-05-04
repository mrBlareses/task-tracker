FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon :cron-service:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/cron-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]