# dockerfile to build image for JBoss EAP 7.4

#start from eap74-openshift
FROM registry.redhat.io/jboss-eap-7/eap74-openjdk8-openshift-rhel7

# setting JVM options
ENV JAVA_OPTS="-XX:+AssumeMP -XX:ParallelGCThreads=16"

# file author / maintainer
LABEL "viyancs" "msofyancs@gmail.com"

# Copy war to deployments folder
COPY backup/after_refactoring.war $JBOSS_HOME/standalone/deployments/ROOT.war

#copy serverconfiguration for the jboss
COPY standalone-full.xml /opt/eap/standalone/configuration/standalone-full.xml
#COPY standalone-openshift.xml /opt/eap/standalone/configuration/standalone-openshift.xml

# User root to modify war owners
USER root

# Modify owners war
RUN chown jboss:jboss $JBOSS_HOME/standalone/deployments/ROOT.war
#RUN chown jboss:jboss /opt/eap/standalone/configuration/standalone-openshift.xml

# Important, use jboss user to run image
USER jboss

RUN ls -lah /opt/eap/standalone/configuration

EXPOSE 9990
EXPOSE 8080
EXPOSE 8443
CMD ["/opt/eap/bin/standalone.sh", "-c", "standalone-full.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]