# syntax=docker/dockerfile:1

# ============================================================================
# Multi-stage build: compile with the full JDK, ship only a slim JRE + jar.
# Produces a small, reproducible image suitable for DigitalOcean App Platform,
# the Container Registry, or a Droplet.
# ============================================================================

# ---- Stage 1: build & test the application ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy the Maven wrapper first and warm the dependency cache. This layer is
# only rebuilt when the build descriptors change, speeding up later builds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Copy sources and build the executable jar (skip tests; CI runs them separately).
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# ---- Stage 2: minimal runtime image ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user for security.
RUN useradd --system --uid 10001 appuser
USER appuser

COPY --from=build /workspace/target/url-shortener-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Container-aware health check hitting Spring Boot Actuator.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
