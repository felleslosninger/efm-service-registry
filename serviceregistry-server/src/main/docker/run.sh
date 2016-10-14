#!/bin/sh

dockerize -wait http://eureka:8761 -wait tcp://logstash:8300

java -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar --spring.profiles.active=${APP_PROFILE} "$@"
