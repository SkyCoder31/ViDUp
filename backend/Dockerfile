# Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Clean build, skipping tests to speed up deployment
RUN mvn clean package -DskipTests

# Run the application
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# CRITICAL: Install FFmpeg inside the Linux container
# This makes the app self-contained. No local dependencies needed.
RUN apk add --no-cache ffmpeg

# Copy the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose the port
EXPOSE 8081

# Run it
ENTRYPOINT ["java", "-jar", "app.jar"]