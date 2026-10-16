FROM docker.io/library/eclipse-temurin:21-jdk

WORKDIR /app

COPY target/quarkus-app/ /app/

EXPOSE 8080

ENV QUARKUS_HTTP_HOST=0.0.0.0
ENV QUARKUS_HTTP_PORT=8080

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]