FROM adoptopenjdk/openjdk11-openj9:jdk-11.28-alpine-slim

EXPOSE 7000
ENTRYPOINT ["java", "-jar", "/app.jar"]

ARG JAR_FILE
ADD target/filesyncserverjavelin-0.0.2-SNAPSHOT.jar app.jar
