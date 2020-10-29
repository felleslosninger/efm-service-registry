FROM openjdk:8-jre-alpine
MAINTAINER Digitaliseringsdirektoratet <lab@digdir.no>

LABEL package="no.digdir" artifact="serviceregistry" version="1.0" description="Digitaliseringsdirektoratet (Digdir)"

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

ENV APP_DIR=/var/lib/digdir \
    APP_JAVA_PARAMS="" \
    APP_PROFILE=dev

#
# Start the application
#

WORKDIR ${APP_DIR}

ADD *.jar ${APP_DIR}/app.jar
ADD run.sh ${APP_DIR}/

RUN chown -R java:java ${APP_DIR}
RUN chmod +x ${APP_DIR}/*

USER java

ENTRYPOINT ["sh", "/var/lib/digdir/run.sh"]
