#!/bin/sh

exec java -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar --spring.profiles.active=${APP_PROFILE} "$@"
