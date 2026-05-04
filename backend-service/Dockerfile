FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon :backend-service:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/backend-service/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]