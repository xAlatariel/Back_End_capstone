# ===================================================================
# MULTI-STAGE BUILD FOR SPRING BOOT APPLICATION
# ===================================================================

# Stage 1: Build stage
FROM openjdk:17-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM openjdk:17-jre-slim AS runtime

# Install dependencies and create app user
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    dumb-init && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    useradd --create-home --shell /bin/bash app

# Set working directory
WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R app:app /app

# Switch to non-root user
USER app

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the application with optimized JVM settings
CMD ["java", \
     "-server", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseG1GC", \
     "-XX:+UseStringDeduplication", \
     "-XX:+OptimizeStringConcat", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-Dspring.profiles.active=prod", \
     "-jar", "app.jar"]

# Labels for metadata
LABEL \
    maintainer="Ristorante Ai Canipai <info@aicanipai.it>" \
    version="1.0.0" \
    description="Backend API for Ristorante Ai Canipai reservation system" \
    org.opencontainers.image.title="Ai Canipai Backend" \
    org.opencontainers.image.description="Spring Boot backend for restaurant management" \
    org.opencontainers.image.vendor="Ristorante Ai Canipai" \
    org.opencontainers.image.version="1.0.0"