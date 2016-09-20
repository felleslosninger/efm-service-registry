FROM openjdk:8-jre-alpine
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi" artifact="serviceregistry" version="1.0" description="Direktoratet for forvaltning og IKT (Difi)"

#
# Open ports for incoming connections
#

EXPOSE 9099

#
# Install application
#

ENV APP_DIR=/var/lib/difi \
    APP_PREFIX=serviceregistry \
    APP_MAIN_CLASS=no.difi.meldingsutveksling.serviceregistry.MoveServiceRegistryApplication \
    APP_JAVA_PARAMS="" \
    APP_PROFILE=dev

ADD ${APP_PREFIX}*.jar ${APP_DIR}/

RUN apk add --update \
	    ca-certificates \
        curl \
	    openssl  && \
	    update-ca-certificates && \
	    rm -rf /var/cache/apk/*

RUN cd /tmp/ && \
    curl -LO "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip" -H 'Cookie: oraclelicense=accept-securebackup-cookie' && \
    unzip jce_policy-8.zip && \
    rm jce_policy-8.zip && \
    yes |cp -v /tmp/UnlimitedJCEPolicyJDK8/*.jar ${JAVA_HOME}/lib/security/

#
# Start the application
#

WORKDIR ${APP_DIR}

CMD APP_NAME=$(ls ${APP_PREFIX}*.jar) && java -jar ${APP_JAVA_PARAMS} ${APP_NAME} ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE}
