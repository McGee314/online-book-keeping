# ============================================================
# Bookkeeping Backend — Multi-stage Docker Build
# Stage 1: Build the JAR with Maven
# Stage 2: Run with minimal JRE Alpine image
# ============================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy POM first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy source and build (skip tests for image)
COPY src ./src
RUN mvn package -DskipTests -B -q

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jre

# Create non-root user for security
RUN groupadd -f -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose backend port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]