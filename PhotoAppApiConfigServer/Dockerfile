FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY PKI/apiEncryptionKey.p12 apiEncryptionKey.p12
# COPY UnlimitedCEPolicyJDK8/* /usr/lib/jvm/java-1.8-openjdk/jre/lib/security # Java JCE for proper encrytion
COPY target/PhotoAppApiConfigServer-0.0.1-SNAPSHOT.jar ConfigServer.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "ConfigServer.jar"]