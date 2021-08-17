FROM azul/zulu-openjdk-alpine:11
VOLUME /tmp
COPY PKI/server.jks server.jks
# COPY UnlimitedCEPolicyJDK8/* /usr/lib/jvm/java-1.8-openjdk/jre/lib/security # Java JCE for proper encrytion
COPY target/PhotoAppApiConfigServer-0.0.1-SNAPSHOT.jar ConfigServer.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "ConfigServer.jar"]