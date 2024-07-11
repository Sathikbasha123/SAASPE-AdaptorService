FROM openjdk:8-jre-stretch
COPY /target/*.jar  app.jar
EXPOSE 8082 9010
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
