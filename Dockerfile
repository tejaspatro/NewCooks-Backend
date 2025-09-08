FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/Backend-0.0.1-SNAPSHOT.jar NewCooks.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "NewCooks.jar"]