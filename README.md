# MICROSERVICES

This repository contains the code of my coursework for Udemy's [Spring Boot Microservices and Spring Cloud](https://www.udemy.com/course/spring-boot-microservices-and-spring-cloud),
plus additional refactoring, changes and extensions that I add as I continue learning about microservices.

## Personal work

- Albums microservice is now a proper service with real database and all the features included in users microservice.
- Improved controller and service layers validation and error control.

## Architecture

The *Photo App* back-end comprehends the following components:
- **Configuration service**: Spring Cloud configuration server. Configuration communication is made with Spring Cloud
Bus and RabbitMQ.
- **Discovery service**: Eureka server for service registration and discovery.
- **API gateway**: Zuul API gateway with load balancing.
- **Users microservice**: user domain operations microservice.
- **Albums microservice**: album domain operations microservice.
- **Account management microservice**: dummy service thought to be the IAM module. Right now, this functionality
is implemented in users microservice (sign up and login) and scattered among the API gateway and microservices
(authorization).
- **MySQL database**: relational database server for user data.
- **Couchbase database**: NoSQL, document database server for album data.
- **Zipkin server**: centralized distributed tracing information. Microservices send tracing information through Sleuth.
- **ELK server**: Elastic Search + Kibana for distributed logging ingestion, processing and visualization. Ingestion
for each microservice is made available through a Logstash pipeline deployed along the microservice.

## Setup

## Execution
