# dockerfile to build image for JBoss EAP 7.0

#start from eap71-openshift
FROM registry.access.redhat.com/jboss-eap-7-tech-preview/eap70

# file author / maintainer
LABEL "viyancs" "msofyancs@gmail.com"

# Copy war to deployments folder
COPY backup/before_refactoring.war $JBOSS_HOME/standalone/deployments/

# User root to modify war owners
USER root

# Modify owners war
RUN chown jboss:jboss $JBOSS_HOME/standalone/deployments/before_refactoring.war

# Important, use jboss user to run image
USER jboss

EXPOSE 9990
CMD ["/opt/eap/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]