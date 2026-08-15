# Multi-stage build: the previous single-stage image copied a jar that had to be
# built on the host first, shipped a full JDK, and ran as root.

# ---------------------------------------------------------------- build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Dependencies resolve in their own layer so source edits do not re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------- runtime
FROM eclipse-temurin:17-jre-alpine

# curl is used by the container healthcheck below.
RUN apk add --no-cache curl \
 && addgroup -S ihub \
 && adduser -S -G ihub ihub

WORKDIR /app

COPY --from=build --chown=ihub:ihub /build/target/*.jar app.jar

USER ihub

EXPOSE 8081

# Respect the container's cgroup memory limit rather than the host's RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
