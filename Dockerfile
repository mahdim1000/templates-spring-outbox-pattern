FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Copy gradle files first for better caching
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application
RUN gradle build --no-daemon -x test

# Production image
FROM eclipse-temurin:21.0.7_6-jre

# Create app user for security
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser

WORKDIR /app

# Install required packages for production monitoring
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# Copy the JAR file
COPY --from=build /app/build/libs/outbox-pattern-1.0.0.jar app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables with sensible defaults
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=production

EXPOSE 8080

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar 