package me.learning.microservices.photoapp.api.albums.data.album;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Configuration
@ConfigurationProperties(prefix = "spring.couchbase")
@Data
@EqualsAndHashCode(callSuper = true)
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

    private String connectionString;

    private String userName;

    private String password;

    private String bucketName;

    private Boolean autoIndexCreation;

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    protected boolean autoIndexCreation() {
        return autoIndexCreation;
    }
}


