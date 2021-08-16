FROM azul/zulu-openjdk-alpine:11
VOLUME /tmp
COPY target/PhotoAppApiGateway-0.0.1-SNAPSHOT.jar ApiGateway.jar
ENTRYPOINT ["java", "-jar", "ApiGateway.jar"]