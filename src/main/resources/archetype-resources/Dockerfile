FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY .mvn .mvn
COPY src src
ARG STORAGE=memory
RUN mvn -B -ntp -Dstorage=${STORAGE} verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/service.jar /app/service.jar
ENV SERVER_ADDRESS=0.0.0.0
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
