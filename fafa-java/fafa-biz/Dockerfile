FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY fafa-java/ fafa-java/
RUN mvn -f fafa-java/pom.xml -pl fafa-biz -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/fafa-java/fafa-biz/target/fafa-biz-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
