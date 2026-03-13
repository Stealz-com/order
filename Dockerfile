# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Install parent pom to local Maven repo (non-recursive)
COPY pom.xml ./parent-pom.xml
RUN mvn install -N -f parent-pom.xml -DskipTests

# Build service as standalone module
WORKDIR /app/order
COPY order/pom.xml ./
RUN mvn dependency:go-offline -DskipTests

COPY order/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=builder /app/order/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3     CMD nc -z localhost 8082 || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
