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
FROM maven:3.8.3-jdk-11
WORKDIR /app
COPY . /app
EXPOSE 8082
CMD ["mvn","compile","vertx:run"]