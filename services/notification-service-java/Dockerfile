FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY services/notification-service-java/pom.xml .
RUN mvn -B dependency:go-offline
COPY services/notification-service-java/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/notification-service.jar app.jar
EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
