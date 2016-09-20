FROM jeanblanchard/java:jre-8
MAINTAINER DIFI <espen.korra@difi.no>

LABEL package="no.difi"
LABEL artifact="serviceregistry"
LABEL version="1.0"
LABEL description="Direktoratet for forvaltning og IKT (Difi)"

#
# Open ports for incoming connections
#

EXPOSE 9099

#
# Install application
#

ENV APP_DIR /var/lib/difi
ENV APP_PREFIX serviceregistry
ENV APP_MAIN_CLASS no.difi.meldingsutveksling.serviceregistry.MoveServiceRegistryApplication
ENV APP_JAVA_PARAMS ""
ENV APP_PROFILE dev

ADD serviceregistry-server/target/${APP_PREFIX}*.jar ${APP_DIR}/

#
# Start the application
#

WORKDIR ${APP_DIR}

CMD APP_NAME=$(ls ${APP_PREFIX}*.jar) && java -jar ${APP_JAVA_PARAMS} ${APP_NAME} ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE}
