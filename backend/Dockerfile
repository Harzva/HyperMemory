### Stage 1: build the Spring Boot application
FROM maven:3.9.5-amazoncorretto-17 AS build
WORKDIR /workspace

# Copy the Maven build descriptors first to leverage caching of dependencies
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline -Dmaven.repo.local=/workspace/.m2/repository

# Copy source and build
COPY src ./src
RUN mvn -B -ntp package -DskipTests -Dmaven.repo.local=/workspace/.m2/repository

### Stage 2: create a lean runtime image
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /workspace/target/campus-rag-backend-0.1.0-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]