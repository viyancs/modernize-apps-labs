####
# This Dockerfile is used in order to build a container that runs the Spring Boot application
#
# Build the image with:
#
# docker build -f docker/Dockerfile -t springboot/sample-demo .
#
# Then run the container using:
#
# docker run -i --rm -p 8081:8081 springboot/sample-demo
####
FROM maven:3.8.3-openjdk-8-slim
FROM openjdk:11-jdk

WORKDIR /build

COPY . .
RUN mvn compile vertx:run
EXPOSE 8082