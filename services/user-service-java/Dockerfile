FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY services/user-service-java/pom.xml .
RUN mvn -B dependency:go-offline
COPY services/user-service-java/src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/user-service.jar app.jar
EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
