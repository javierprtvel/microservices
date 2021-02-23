package me.learning.microservices.photoapp.api.users.ui.controllers;

import static me.learning.microservices.photoapp.api.users.ui.controllers.utils.UsersControllerResponseUtils.createdUserResponseEqualsWithoutUserId;
import static me.learning.microservices.photoapp.api.users.ui.controllers.utils.UsersControllerResponseUtils.userResponseEqualsWithoutAlbums;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.user.Role;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.netflix.discovery.EurekaClient;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import me.learning.microservices.photoapp.api.users.data.User;
import me.learning.microservices.photoapp.api.users.data.UsersRepository;
import me.learning.microservices.photoapp.api.users.service.exception.UserAlreadyExistsException;
import me.learning.microservices.photoapp.api.users.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.users.ui.controllers.integration.Album;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserRequest;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Transactional
@Testcontainers
@ActiveProfiles({ "test", "security-disabled" })
class UsersControllerIntegrationTest {

    private static final String COUCHBASE_SERVICE_NAME = "couchbase-db";

    private static final BucketDefinition albumsBucketDefinition;

    @Container
    private static final CouchbaseContainer couchbaseContainer;

    private static final String DISCOVERY_SERVICE_NAME = "eureka-server";

    private static final Integer DISCOVERY_SERVICE_PORT = 8010;

    @Container
    private static final GenericContainer discoveryServiceContainer;

    private static final String ALBUMS_SERVICE_NAME = "albums-ws";

    private static final Integer ALBUMS_SERVICE_PORT = 54401;

    @Container
    private static final GenericContainer albumsServiceContainer;

    private static String albumsTestsUserId;

    private final UsersController usersController;

    private final UsersRepository usersRepository;

    @Autowired
    @Lazy
    private EurekaClient eurekaClient;

    // because this albums service instance has to be already registered with Eureka before starting each test
    @Value("${timeouts-ms.discovery-service-registration}")
    private Integer discoveryServiceRegistrationTimeoutMs;

    static {
        Network network = Network.newNetwork();

        /* Couchbase container
         *
         * Cannot use community image because it does not support "memory_optimized" storage mode
         * and this mode is the one used in CouchbaseContainer, and it is not configurable at the moment:
         * https://github.com/testcontainers/testcontainers-java/issues/1440
         */
        DockerImageName couchbaseImageName = DockerImageName.parse("couchbase/server:enterprise-6.6.2");

        albumsBucketDefinition = new BucketDefinition("albums")
            .withPrimaryIndex(true);

        couchbaseContainer = new CouchbaseContainer(couchbaseImageName)
            .withNetwork(network)
            .withCredentials("Administrator", "administrator")
            .withBucket(albumsBucketDefinition)
            .withCreateContainerCmdModifier(createContainerCmd -> {
                createContainerCmd.withHostName(COUCHBASE_SERVICE_NAME);
                createContainerCmd.withName(COUCHBASE_SERVICE_NAME);
            });;

        // Discovery service container
        DockerImageName discoveryServiceImageName = DockerImageName.parse("jopv/eureka-server");

        discoveryServiceContainer = new GenericContainer(discoveryServiceImageName.toString())
            .withExposedPorts(DISCOVERY_SERVICE_PORT)
            .withNetwork(network)
            .waitingFor(
                Wait.forHttp("/actuator/health")
                    .forPort(DISCOVERY_SERVICE_PORT)
            )
            .withEnv("spring.profiles.active", "test")
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> {
                createContainerCmd.withHostName(DISCOVERY_SERVICE_NAME);
                createContainerCmd.withName(DISCOVERY_SERVICE_NAME);
            });

        /* Albums service container
         *
         * The users service being tested runs in the host environment, and will attempt to access albums service using
         * the hostname and port returned by Eureka.
         * Services register in Eureka with the application (container) port, not the port generated by Testcontainers
         * library to do forwarding, so it must be ensured that the container exposes the container port with the same
         * host port. We do this by instantiating FixedHostPortGenericContainer instead of GenericContainer.
         */
        DockerImageName usersServiceImageName = DockerImageName.parse("jopv/albums-ws");

        albumsServiceContainer = new FixedHostPortGenericContainer(usersServiceImageName.toString())
            .withFixedExposedPort(ALBUMS_SERVICE_PORT, ALBUMS_SERVICE_PORT)
            .withExposedPorts(ALBUMS_SERVICE_PORT)
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/albums/status/check").forPort(ALBUMS_SERVICE_PORT))
            .withEnv("server.port", ALBUMS_SERVICE_PORT.toString())
            .withEnv("spring.profiles.active", "test,security-disabled")
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> {
                createContainerCmd.withHostName(ALBUMS_SERVICE_NAME);
                createContainerCmd.withName(ALBUMS_SERVICE_NAME);
            });

        /* For connecting one container to another container in the same network, the hostname is the container
         * name (or IP address) and the port is the exposed port, not the random port forwarded by Testcontainers library
         */

        // Start containers with sufficient delay between each other so applications can startup properly
        couchbaseContainer.start();

        String couchbaseConnectionString = String.format(
            "couchbase://%s",
            COUCHBASE_SERVICE_NAME
        );
        albumsServiceContainer.addEnv("spring.couchbase.connection-string", couchbaseConnectionString);
        albumsServiceContainer.addEnv("spring.couchbase.user-name", "photoapp_dbuser");
        albumsServiceContainer.addEnv("spring.couchbase.password", "photoapp_dbuser");

        discoveryServiceContainer.start();

        String discoveryServiceRegistrationUrl = String.format(
            "http://%s:%s/eureka",
            DISCOVERY_SERVICE_NAME,
            DISCOVERY_SERVICE_PORT
        );
        albumsServiceContainer.addEnv("eureka.client.serviceUrl.defaultZone", discoveryServiceRegistrationUrl);

        populateCouchbaseDatabase();

        albumsServiceContainer.start();
    }

    @Autowired
    UsersControllerIntegrationTest(UsersController usersController, UsersRepository usersRepository) {
        this.usersController = usersController;
        this.usersRepository = usersRepository;
    }

    @DynamicPropertySource
    static void setUpPropertiesForExternalServices(DynamicPropertyRegistry registry) {
        discoveryServiceContainer.start();
        String discoveryServiceUrl = String.format(
            "http://localhost:%s/eureka",
            discoveryServiceContainer.getMappedPort(DISCOVERY_SERVICE_PORT)
        );
        registry.add("eureka.client.serviceUrl.defaultZone", () -> discoveryServiceUrl);
    }

    @BeforeEach
    void waitForDiscoveryServiceRegistration() {
        await()
            .atMost(Duration.ofMillis(discoveryServiceRegistrationTimeoutMs))
            .until(() -> eurekaClient.getApplications().size() > 0);
    }

    @Test
    void get_status_message() {
        String statusMsg = usersController.status();

        assertThat(statusMsg)
            .isNotBlank();
    }

    @Test
    void get_user_without_albums_ok() throws UserNotFoundException {
        User createdUser = createTestUser();

        ResponseEntity<UserResponse> result = usersController.getUser(createdUser.getUserId(), false);

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.OK);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
    }

    @Test
    void get_user_without_albums_not_found() {
        createTestUser();

        assertThatThrownBy(() -> usersController.getUser("2", false))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @Disabled("Disabled until the problem with Couchbase query from albums microservice container is solved")
    void get_user_with_albums_ok() throws UserNotFoundException {
        User createdUser = createAlbumsTestUser();

        ResponseEntity<UserResponse> result = usersController.getUser(createdUser.getUserId(), true);

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.OK);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .isNotNull()
            .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
        assertThat(result.getBody().getAlbums())
            .isNotEmpty()
            .allMatch(albumResponse -> createdUser.getUserId().equals(albumResponse.getUserId()));
    }

    @Test
    void get_user_with_albums_external_service_failure() throws UserNotFoundException {
        User createdUser = createTestUser();

        ResponseEntity<UserResponse> result = usersController.getUser(createdUser.getUserId(), true);

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.OK);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .isNotNull()
            .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
        assertThat(result.getBody().getAlbums())
            .isEmpty();
    }

    @Test
    void get_user_with_albums_not_found() {
        createTestUser();

        assertThatThrownBy(() -> usersController.getUser("2", true))
            .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_user_ok() throws UserAlreadyExistsException {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setPassword("12345678");

        CreateUserResponse expectedCreatedUserResponse = new CreateUserResponse();
        expectedCreatedUserResponse.setEmail("test@example.com");
        expectedCreatedUserResponse.setFirstName("John");
        expectedCreatedUserResponse.setLastName("Doe");

        ResponseEntity<CreateUserResponse> result = usersController.createUser(createUserRequest);

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.CREATED);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .matches(createdResponse -> createdUserResponseEqualsWithoutUserId(expectedCreatedUserResponse, createdResponse));
    }

    @Test
    void create_user_already_exists() {
        User alreadyExistsUser = createTestUser();

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail(alreadyExistsUser.getEmail());
        createUserRequest.setFirstName("Sally");
        createUserRequest.setLastName("Smith");
        createUserRequest.setPassword("12345678");

        assertThatThrownBy(() -> usersController.createUser(createUserRequest)).isInstanceOf(UserAlreadyExistsException.class);
    }

    private User createTestUser() {
        String userId = UUID.randomUUID().toString();

        User u = new User();
        u.setEmail("test@example.com");
        u.setUserId(userId);
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setEncryptedPassword("DSFDFHGDFKGHHCXVHBSD$dghlkdh");

        return this.usersRepository.save(u);
    }

    private User createAlbumsTestUser() {
        String userId = albumsTestsUserId;

        User u = new User();
        u.setEmail("albumstest@example.com");
        u.setUserId(userId);
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setEncryptedPassword("DSFDFHGDFKGHHCXVHBSD$xbvcbk");

        return this.usersRepository.save(u);
    }

    private static void populateCouchbaseDatabase() {
        couchbaseContainer.start();

        ClusterEnvironment clusterEnvironment = ClusterEnvironment.builder()
            .timeoutConfig(TimeoutConfig.kvTimeout(Duration.ofSeconds(5)))
            .build();
        ClusterOptions clusterOptions = ClusterOptions.clusterOptions(
            couchbaseContainer.getUsername(),
            couchbaseContainer.getPassword()
        )
            .environment(clusterEnvironment);

        Cluster cluster = Cluster.connect(couchbaseContainer.getConnectionString(), clusterOptions);

        com.couchbase.client.java.manager.user.User user = new com.couchbase.client.java.manager.user.User("photoapp_dbuser")
            .displayName("photoapp_dbuser")
            .password("photoapp_dbuser");
        String albumsBucketName = albumsBucketDefinition.getName();
        user.roles(
            // Roles required for the reading of data from the bucket
            new Role("data_reader", "*"),
            new Role("query_select", "*"),
            // Roles required for the writing of data into the bucket.
            new Role("data_writer", albumsBucketName),
            new Role("query_insert", albumsBucketName),
            new Role("query_delete", albumsBucketName),
            // Role required for the creation of indexes on the bucket.
            new Role("query_manage_index", albumsBucketName));

        cluster.users().upsertUser(user);

        albumsTestsUserId = UUID.randomUUID().toString();
        String album1Id = "45762459vcxhkkg";
        String album2Id = "45762459vcxjbnh";
        String album3Id = "45762459cxvbyqw";
        Album album1 = new Album(null, album1Id, albumsTestsUserId, "Test Album 1", "Album for albums microservice integration tests");
        Album album2 = new Album(null, album2Id, albumsTestsUserId, "Test Album 2", "Album for albums microservice integration tests");
        Album album3 = new Album(null, album3Id, albumsTestsUserId, "Test Album 3", "Album for albums microservice integration tests");

        Collection albumsDefaultCollection = cluster.bucket(albumsBucketDefinition.getName())
            .defaultCollection();

        albumsDefaultCollection.upsert(albumsTestsUserId + "-" + album1Id, album1);
        albumsDefaultCollection.upsert(albumsTestsUserId + "-" + album2Id, album2);
        albumsDefaultCollection.upsert(albumsTestsUserId + "-" + album3Id, album3);

        cluster.disconnect();
        clusterEnvironment.shutdown();
    }
}