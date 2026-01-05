# Arcata API - Multi-stage Docker build
# Build: bun run docker:build:api

# Stage 1: Builder
FROM eclipse-temurin:21-jdk-alpine AS builder

RUN apk add --no-cache curl

WORKDIR /build

# Copy mill wrapper and build files
COPY mill mill
COPY build.sc build.sc
COPY .mill-version .mill-version
COPY src/ src/

# Build the assembly JAR
RUN chmod +x mill && ./mill api.assembly

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install curl for healthchecks, pandoc for DOC/DOCX/RTF text extraction
RUN apk add --no-cache curl pandoc

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/out/api/assembly.dest/out.jar /app/api.jar

# Create non-root user for security
RUN addgroup -g 1001 -S arcata && \
    adduser -u 1001 -S arcata -G arcata && \
    chown -R arcata:arcata /app

USER arcata

# Expose default port
EXPOSE 4203

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:4203/api/v1/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Use Java to run the JAR
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/api.jar"]
