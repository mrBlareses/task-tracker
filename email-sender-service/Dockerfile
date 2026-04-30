FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd -r -u 1001 appuser

COPY build/libs/*.jar app.jar

EXPOSE 8083

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]