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
FROM openjdk:11-jdk
###########################################################################
# non-root user:
###########################################################################

# Add a non-root user to prevent files being created with root permissions on host machine.
ARG PUID=1000
ARG PGID=1000
ARG MAVEN_USER="mvn-user"
RUN set -xe; \
    groupadd -g ${PGID} ${MAVEN_USER} && \
    useradd -u ${PUID} -g ${MAVEN_USER} -m ${MAVEN_USER} -G ${MAVEN_USER} && \
    usermod -p "*" ${MAVEN_USER} -s /bin/bash 

# Install Maven
ARG MAVEN_VERSION=3.8.3
ARG USER_HOME_DIR="/home/${MAVEN_USER}/"
ARG SHA=1c12a5df43421795054874fd54bb8b37d242949133b5bf6052a063a13a93f13a20e6e9dae2b3d85b9c7034ec977bbc2b6e7f66832182b9c863711d78bfe60faa
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

RUN mkdir /var/www
WORKDIR /var/www
COPY . /var/www

RUN echo "PWD is: $PWD"
RUN echo "WHOAMI is: $WHOAMI"
RUN ls -lah

COPY mvn-entrypoint.sh /usr/local/bin/mvn-entrypoint.sh
COPY settings-docker.xml /usr/share/maven/ref/
RUN chmod +x /usr/local/bin/mvn-entrypoint.sh

EXPOSE 8082

ENTRYPOINT ["/usr/local/bin/mvn-entrypoint.sh"]
CMD ["mvn","compile","vertx:run"]