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
    APP_JAVA_PARAMS="" \
    APP_PROFILE=dev \
    DOCKERIZE_VERSION=v0.2.0

RUN apk add --update \
        curl \
        tini \
        ca-certificates \
        openssl  && \
        update-ca-certificates && \
	rm -rf /var/cache/apk/* && \
        wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz && \
        tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

#
# Start the application
#

WORKDIR ${APP_DIR}

ENTRYPOINT ["/sbin/tini", "-g", "--", "./run.sh"]

ADD *.jar ${APP_DIR}/app.jar
ADD run.sh ${APP_DIR}/
