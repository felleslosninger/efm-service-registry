FROM openjdk:11-jre-slim

LABEL package="no.digdir" artifact="serviceregistry" version="1.0" description="Digitaliseringsdirektoratet (Digdir)"

RUN groupadd -o -g 1000 java \
    && useradd -o -r -m -u 1000 -g 1000 java

RUN chown -R java:java /opt

RUN mkdir /logs && chown -R java:java /logs

RUN apt-get update && apt-get upgrade -y && apt-get install -y

#
# Open ports for incoming connections
#

EXPOSE 9099 9199

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

ADD target/*.jar ${APP_DIR}/app.jar

RUN chown -R java:java ${APP_DIR}
RUN chmod +x ${APP_DIR}/*

USER java

ENTRYPOINT ["sh", "-c",  "java -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar --spring.profiles.active=${APP_PROFILE}", "$@"]
