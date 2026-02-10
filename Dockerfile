# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy parent pom.xml first
COPY pom.xml ./

# Copy order-service pom.xml
COPY order-service/pom.xml ./order-service/

# Download dependencies (layer caching optimization)
RUN mvn dependency:go-offline -pl order-service -am

# Copy source code
COPY order-service/src ./order-service/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -pl order-service -am -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add metadata
LABEL maintainer="ecommerce-team"
LABEL service="order-service"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/order-service/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Environment variables for Spring profiles and JVM optimization
ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose the service port
EXPOSE 8081

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
