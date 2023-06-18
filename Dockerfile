FROM openjdk:17-jdk-slim
WORKDIR /opt
ENV PORT 8100
EXPOSE 8100
COPY build/libs/elasticsearch-interactive-service.jar /opt/elasticsearch-interactive-service.jar
ENTRYPOINT exec java $JAVA_OPTS -jar elasticsearch-interactive-service.jar