package me.learning.microservices.photoapp.api.albums;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableCouchbaseRepositories
@EnableFeignClients
public class PhotoAppApiAlbumsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotoAppApiAlbumsApplication.class, args);
	}

}

