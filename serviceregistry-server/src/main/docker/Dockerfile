FROM openjdk:8-jre-alpine
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi" artifact="serviceregistry" version="1.0" description="Direktoratet for forvaltning og IKT (Difi)"

RUN addgroup -g 1001 java  && adduser -u 1001 -S java -G java

RUN chown -R java:java /opt

RUN mkdir /logs && chown -R java:java /logs

#
# Open ports for incoming connections
#

EXPOSE 9099

#
# Install application
#

ENV APP_DIR=/var/lib/difi \
    APP_JAVA_PARAMS="" \
    APP_PROFILE=dev

RUN apk add --update \
        curl \
        tini \
        ca-certificates \
        openssl  && \
        update-ca-certificates && \
	rm -rf /var/cache/apk/*

#
# Start the application
#

WORKDIR ${APP_DIR}

ADD *.jar ${APP_DIR}/app.jar
ADD run.sh ${APP_DIR}/

RUN chown -R java:java /var/lib/difi
RUN chmod +x ${APP_DIR}/*

USER java

RUN ls -l /var/lib/difi/run.sh

ENTRYPOINT ["sh", "/var/lib/difi/run.sh"]
