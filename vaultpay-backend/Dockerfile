# ============================================================
# VaultPay Backend — Multi-stage Docker Build
# ============================================================
# WHAT IS A MULTI-STAGE BUILD?
# Instead of shipping the entire JDK (compiler + tools + source code),
# we compile in Stage 1 (big image), then copy ONLY the .jar file
# into Stage 2 (small image). Result: ~300MB instead of ~800MB.
# ============================================================

# ──── Stage 1: BUILD ────
# Uses the full JDK (Java Development Kit) to compile the code
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml FIRST (dependency caching trick!)
# Docker caches each layer. If pom.xml hasn't changed, Docker
# reuses the cached dependencies → much faster rebuilds
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable (needed on Linux)
RUN chmod +x mvnw

# Download all dependencies (cached unless pom.xml changes)

# NOW copy source code (this layer changes frequently)
COPY src/ src/

# Build the .jar file, skip tests (we test separately)
# -B = batch mode (no interactive prompts)
RUN ./mvnw package -DskipTests -B

# ──── Stage 2: RUN ────
# Uses only JRE (Java Runtime Environment) — no compiler needed
# This is the image that actually runs in production
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the compiled .jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Document which port the app uses (doesn't actually publish it)
EXPOSE 8080

# Health check — Docker will restart the container if this fails
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/v3/api-docs || exit 1

# Start the application
# Using exec form (JSON array) instead of shell form
# so signals (SIGTERM) are forwarded correctly for graceful shutdown
ENTRYPOINT ["java", "-jar", "app.jar"]
