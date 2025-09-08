FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/Backend-0.0.1-SNAPSHOT.jar NewCooks.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "NewCooks.jar"]
