FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY fafa-java/ fafa-java/
RUN mvn -f fafa-java/pom.xml -pl fafa-gateway -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/fafa-java/fafa-gateway/target/fafa-gateway-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
