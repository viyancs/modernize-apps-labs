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

# Install Maven
RUN mkdir /var/www
RUN mkdir -p /data/maven/.m2
RUN chmod 775 -R /data
RUN whoami 
RUN ls -lah /data
RUN cat /usr/share/maven/conf/settings.xml

WORKDIR /var/www
COPY . /var/www
RUN chmod 775 -R /var/www
RUN ls -lah

COPY settings-docker.xml /usr/share/maven/conf/settings.xml
RUN cat /usr/share/maven/conf/settings.xml

EXPOSE 8082
CMD ["mvn","compile","vertx:run"]