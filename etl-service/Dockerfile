FROM maven:3.9.7-eclipse-temurin-17
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=0 /app/target/etl-service.jar app.jar
CMD ["java", "-jar", "app.jar"]
