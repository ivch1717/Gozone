FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

ARG SERVICE
COPY ${SERVICE}/pom.xml ./pom.xml
COPY ${SERVICE}/.mvn ./.mvn
COPY ${SERVICE}/mvnw ./mvnw
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY ${SERVICE}/src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -ntp -Dmaven.test.skip=true package && \
    cp target/*-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:25-jre-jammy AS runtime
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar ./app.jar
USER 10001:10001
ENV SERVER_PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=6 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
