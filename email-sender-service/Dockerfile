FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon :email-sender-service:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/email-sender-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]