# Microservices
This repository contains the code of my coursework for Udemy's [Spring Boot Microservices and Spring Cloud](https://www.udemy.com/course/spring-boot-microservices-and-spring-cloud) plus additional refactoring, changes and extensions that I add as I continue learning about microservices.

## Beyond the course
- Updated to Spring Boot 2.5.0 and Java 11.
- Albums microservice is a full-fledged service with a real database and all the features included in users microservice
(authorization with JWT, distributed tracing, Feign for microservice communication, etc.).
- Improved validation and error control in controller and service layers.
- Migrated from Zuul to Spring Cloud API Gateway.
- Migrated from Hystrix to Resilience4j.
- Automated testing.
- Added Lombok to avoid boilerplate code.
- Substituted ModelMapper with Mapstruct.
- General code refactoring.

## Architecture
The *Photo App* back-end comprehends the following components:
- **Configuration service**: Spring Cloud configuration server. It supplies external configuration
via Spring Cloud Bus and RabbitMQ middleware.
- **Discovery service**: Eureka server for service registration and discovery.
- **API gateway**: Spring Cloud API gateway with load balancing.
- **Users microservice**: user domain operations microservice.
- **Albums microservice**: album domain operations microservice.
- **Account management microservice**: dummy service conceived as the IAM module. Right now, this functionality
is implemented in users microservice (sign up and login) and scattered among the API gateway and microservices
(authorization).
- **MySQL database**: relational database server for user data.
- **Couchbase database**: NoSQL, document database server for album data.
- **Zipkin server**: centralized tracing information server. Microservices send tracing information through Sleuth.
- **ELK server**: Elastic Search + Kibana for distributed logging ingestion, processing and visualization. Ingestion
for each microservice is made available through a Logstash pipeline deployed along the microservice.

## Build
You can build configuration service, discovery service, API gateway and every microservice as JAR files -Spring Boot standalone
applications- by executing ``mvn package [-DskipTests]`` on the root directory of the correspondent module.

After that, you can build a Docker image executing ```docker build .``` on the same directory, which contains the Dockerfile.

Regarding the third-party components like databases, tracing information server and centralized logging server, you need to
install, setup and run the required software, either as a host program or a Docker container.

## Execution
For the *Photo App* application to work correctly upon the startup, you must ensure the components start in the following order,
with a sufficient delay between dependent units:

1. RabbitMQ
2. Configuration service
3. Discovery service
4. Databases
5. Zipkin server (unless disabled in the setup)
6. ELK server (optional)
7. Microservices
8. Spring Cloud API gateway

If a component exits or works incorrectly due to intercommunication or synchronization issues with others, try to bus-refresh
(``<config-server-hostname>:<config-server-port>/actuator/bus-refresh``) or restart the affected units. An example is when
users-ws, albums-ws and/or API gateway instances may not have registered properly in Eureka because the discovery server was not
ready when the client requested registration: do bus-refresh or restart the affected clients.

**Note**: at the moment, Couchbase database has to be configured manually (cluster, bucket, application user and indexes),
otherwise the albums microservice will not work. 

### On-premise
If you want to launch the application on the host machine, just run the JAR file for each component -configuration can be
overridden with environment variables- sticking with the correct startup order.

Notice that configuration server is disabled by default on all configuration clients.

### Containers in localhost
If you prefer to keep your host machine clean, you can launch the full *Photo App* in Docker containers by executing the
following command in the project root directory:

``docker-compose -p "<projectname>" up -d``

Again, if an intercommunication or synchronization error happens at the startup, try to bus-refresh or restart the affected units.

The details of the Docker app can be found in the file [``docker-compose.yml``](./docker-compose.yml) in the root directory. Notice
that it does not include the ELK components because of the total amount of memory that would be required. You can remove the Zipkin
service as well to reduce memory consumption.

### Containers in the Cloud
Each component includes a Dockerfile in the root directory of its module. You can build an image, upload it to
you Docker Hub account and then run it on a container in the Cloud (e.g. AWS EC2 instances).

Just **be sure to override the configuration regarding external component hostnames, IPs, URIs, credentials, etc**. For example, set
RabbitMq host property in Config Server to correspondent RabbitMq instance IP:

```
RabbitMQ instance with IP 172.17.0.2:
       docker run -d -p 15672:15672 -p 5672:5672 -p 15671:15671 -p 5671:5671 -p 4369:4369 --name rabbitmq \
  <your_dockerhub>/rabbitmq:3-management
  
Config Server instance:
  	docker run -d -p 8012:8012 --name config-server \
  -v "/home/ec2-user/local-config:/config" \
  -e "spring.rabbitmq.host=172.17.0.2" \
  -e "spring.cloud.config.server.native.searchLocations=file:/config" \
  <your_dockerhub>/config-server
```

## Testing
Users and albums microservices have **Spring Boot integration tests** that test against databases and external services running in
containers, using **Testcontainers** library.

Besides, there is a *Postman* collection JSON for API testing at the [Postman folder](test/postman).
