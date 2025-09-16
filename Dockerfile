# Build stage
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY data ./data
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-slim
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/data ./data
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]