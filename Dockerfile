# Use official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy jar file (adjust the jar name if needed)
COPY target/Backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port your app listens on (8080 by default)
EXPOSE 8080

# Set environment variables (optional defaults, overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=prod

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
