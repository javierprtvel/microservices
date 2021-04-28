package me.learning.microservices.photoapp.api.albums.ui.controllers;

import static me.learning.microservices.photoapp.api.albums.ui.controllers.utils.AlbumsControllerResponseUtils.albumResponseEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.user.Role;
import com.couchbase.client.java.manager.user.User;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.netflix.discovery.EurekaClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import me.learning.microservices.photoapp.api.albums.data.Album;
import me.learning.microservices.photoapp.api.albums.data.AlbumsRepository;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumNotFoundException;
import me.learning.microservices.photoapp.api.albums.service.exception.AlbumServiceException;
import me.learning.microservices.photoapp.api.albums.service.exception.UserNotFoundException;
import me.learning.microservices.photoapp.api.albums.ui.controllers.integration.UserCreationRequest;
import me.learning.microservices.photoapp.api.albums.ui.controllers.integration.UserCreationResponse;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;
import me.learning.microservices.photoapp.api.albums.ui.model.CreateAlbumRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
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
@Testcontainers
@ActiveProfiles({ "test", "security-disabled" })
class AlbumsControllerIntegrationTest {

    private static final BucketDefinition albumsBucketDefinition;

    @Container
    private static final CouchbaseContainer couchbaseContainer;

    private static final String DISCOVERY_SERVICE_NAME = "eureka-server";

    private static final Integer DISCOVERY_SERVICE_PORT = 8010;

    @Container
    private static final GenericContainer discoveryServiceContainer;

    private static final String USERS_SERVICE_NAME = "users-ws";

    private static final Integer USERS_SERVICE_PORT = 54400;

    @Container
    private static final GenericContainer usersServiceContainer;

    private static String createdTestUserId;

    private final AlbumsController albumsController;

    private final AlbumsRepository albumsRepository;

    @Autowired
    @Lazy
    private EurekaClient eurekaClient;

    // because this albums service instance has to be already registered with Eureka before starting each test
    @Value("${timeouts-ms.discovery-service-registration}")
    private Integer discoveryServiceRegistrationTimeoutMs;

    // because of Couchbase eventual consistency
    @Value("${timeouts-ms.couchbase-db-consistency}")
    private Integer couchbaseDbConsistencyTimeoutMs;

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
            .withBucket(albumsBucketDefinition);

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

        /* Users service container
         *
         * The albums service being tested runs in the host environment, and will attempt to access users service using
         * the hostname and port returned by Eureka.
         * Services register in Eureka with the application (container) port, not the port generated by Testcontainers
         * library to do forwarding, so it must be ensured that the container exposes the container port with the same
         * host port. We do this by instantiating FixedHostPortGenericContainer instead of GenericContainer.
         */
        DockerImageName usersServiceImageName = DockerImageName.parse("jopv/users-ws");

        usersServiceContainer = new FixedHostPortGenericContainer(usersServiceImageName.toString())
            .withFixedExposedPort(USERS_SERVICE_PORT, USERS_SERVICE_PORT)
            .withExposedPorts(USERS_SERVICE_PORT)
            .withNetwork(network)
            .waitingFor(
                Wait.forHttp("/users/status/check")
                    .forPort(USERS_SERVICE_PORT)
            )
            .withEnv("server.port", USERS_SERVICE_PORT.toString())
            .withEnv("spring.profiles.active", "test,security-disabled")
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) createContainerCmd -> {
                createContainerCmd.withHostName(USERS_SERVICE_NAME);
                createContainerCmd.withName(USERS_SERVICE_NAME);
            });

        /* For connecting one container to another container in the same network, the hostname is the container
         * name (or IP address) and the port is the exposed port, not the random port forwarded by Testcontainers library
         */

        // Start containers with sufficient delay between each other so applications can startup properly
        couchbaseContainer.start();

        discoveryServiceContainer.start();

        String discoveryServiceRegistrationUrl = String.format(
            "http://%s:%s/eureka",
            DISCOVERY_SERVICE_NAME,
            DISCOVERY_SERVICE_PORT
        );
        usersServiceContainer.addEnv("eureka.client.serviceUrl.defaultZone", discoveryServiceRegistrationUrl);

        usersServiceContainer.start();
    }

    @Autowired
    public AlbumsControllerIntegrationTest(AlbumsController albumsController, AlbumsRepository albumsRepository) {
        this.albumsController = albumsController;
        this.albumsRepository = albumsRepository;
    }

    @DynamicPropertySource
    static void setUpPropertiesForExternalServices(DynamicPropertyRegistry registry) {
        couchbaseContainer.start();
        registry.add("spring.couchbase.connection-string", couchbaseContainer::getConnectionString);

        discoveryServiceContainer.start();
        String discoveryServiceUrl = String.format(
            "http://localhost:%s/eureka",
            discoveryServiceContainer.getMappedPort(DISCOVERY_SERVICE_PORT)
        );
        registry.add("eureka.client.serviceUrl.defaultZone", () -> discoveryServiceUrl);
    }

    @BeforeAll
    static void setUpCouchbaseAndUsersService() {
        populateCouchbaseDatabase();
        populateUsersService();
    }

    @BeforeEach
    void waitForDiscoveryServiceRegistration() {
        await()
            .atMost(Duration.ofMillis(discoveryServiceRegistrationTimeoutMs))
            .until(() -> eurekaClient.getApplications().size() > 0);
    }

    @AfterEach
    void deleteAllAlbums() {
        albumsRepository.deleteAll();
    }

    @Test
    void status_returns_status_message() {
        String statusMsg = albumsController.status();

        assertThat(statusMsg)
            .isNotBlank();
    }

    @Test
    void get_all_albums_ok() {
        List<Album> createdAlbums = createAlbumsForUser("1");

        List<AlbumResponse> result = albumsController.getAllAlbums();

        assertThat(result)
            .isNotEmpty()
            // another test could have created albums concurrently
            .hasSizeGreaterThan(createdAlbums.size())
            .filteredOn(albumResponse -> createdAlbums.stream()
                .anyMatch(createdAlbum -> createdAlbum.getId().equals(albumResponse.getId()))
            )
            .allMatch(albumResponse -> createdAlbums.stream()
                .anyMatch(createdAlbum -> albumResponseEquals(createdAlbum, albumResponse))
            );
    }

    @Test
    void get_album_ok() throws AlbumNotFoundException {
        Album createdAlbum = createAlbum();

        ResponseEntity<AlbumResponse> result = albumsController.getAlbum(createdAlbum.getId());

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.OK);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .isNotNull()
            .matches(albumResponse -> albumResponseEquals(createdAlbum, albumResponse));
    }

    @Test
    void get_album_not_found() {
        createAlbum();

        assertThatThrownBy(() -> albumsController.getAlbum("2")).isInstanceOf(AlbumNotFoundException.class);
    }

    @Test
    void get_user_albums_ok() {
        String userId = UUID.randomUUID().toString();
        List<Album> userAlbums = createAlbumsForUser(userId);

        List<AlbumResponse> result = albumsController.getUserAlbums(userId);

        assertThat(result)
            .isNotEmpty()
            .filteredOn(albumResponse -> userAlbums.stream().anyMatch(album -> album.getId().equals(albumResponse.getId())))
            .isNotEmpty();
    }

    @Test
    void get_user_albums_user_not_found() {
        String userId = UUID.randomUUID().toString();
        createAlbumsForUser(userId);

        List<AlbumResponse> result = albumsController.getUserAlbums("2");

        assertThat(result)
            .isEmpty();
    }

    @Test
    void create_user_album_ok() throws AlbumServiceException {
        String userId = createdTestUserId;
        CreateAlbumRequest createAlbumRequest = new CreateAlbumRequest();
        createAlbumRequest.setUserId(userId);
        createAlbumRequest.setName("Test album");
        createAlbumRequest.setDescription("Album used for integration tests");

        ResponseEntity<AlbumResponse> result = albumsController.createUserAlbum(userId, createAlbumRequest);

        assertThat(result)
            .isNotNull()
            .extracting(ResponseEntity::getStatusCode)
            .isEqualTo(HttpStatus.CREATED);
        assertThat(result)
            .extracting(ResponseEntity::getBody)
            .matches(albumResponse -> StringUtils.isNotBlank(albumResponse.getId())
                && StringUtils.isNotBlank(albumResponse.getAlbumId())
                && createAlbumRequest.getName().equals(albumResponse.getName())
                && createAlbumRequest.getDescription().equals(albumResponse.getDescription())
            );
    }

    @Test
    void create_user_album_user_not_found() {
        String userId = UUID.randomUUID().toString();
        CreateAlbumRequest createAlbumRequest = new CreateAlbumRequest();
        createAlbumRequest.setUserId(userId);
        createAlbumRequest.setName("Test album");
        createAlbumRequest.setDescription("Album used for integration tests");

        assertThatThrownBy(() -> albumsController.createUserAlbum(userId, createAlbumRequest)).isInstanceOf(UserNotFoundException.class);
    }

    private Album createAlbum() {
        Album album = new Album();
        album.setAlbumId(UUID.randomUUID().toString());
        album.setUserId(UUID.randomUUID().toString());
        album.setDescription("Album used for integration tests");
        album.setName("Test Album");

        Album createdAlbum = albumsRepository.save(album);

        await()
            .atMost(Duration.ofMillis(couchbaseDbConsistencyTimeoutMs))
            .until(() -> albumsRepository.findById(createdAlbum.getId()).isPresent());

        return createdAlbum;
    }

    private List<Album> createAlbumsForUser(String userId) {
        Album album1 = new Album();
        album1.setAlbumId(UUID.randomUUID().toString());
        album1.setUserId(userId);
        album1.setDescription("Album used for integration tests");
        album1.setName("Test Album");
        Album album2 = new Album();
        album2.setAlbumId(UUID.randomUUID().toString());
        album2.setUserId(userId);
        album2.setDescription("Album used for integration tests");
        album2.setName("Test Album");
        Album album3 = new Album();
        album3.setAlbumId(UUID.randomUUID().toString());
        album3.setUserId(userId);
        album3.setDescription("Album used for integration tests");
        album3.setName("Test Album");

        Iterable<Album> createdAlbumsIterable = albumsRepository.saveAll(Arrays.asList(album1, album2, album3));
        List<Album> createdAlbums = new ArrayList<>();
        createdAlbumsIterable.forEach(createdAlbums::add);

        await()
            .atMost(Duration.ofMillis(couchbaseDbConsistencyTimeoutMs))
            .until(() -> !albumsRepository.findAll().isEmpty());

        return createdAlbums;
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

        User user = new User("albums")
            .displayName("albums")
            .password("albums");
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

        cluster.disconnect();
        clusterEnvironment.shutdown();
    }

    private static void populateUsersService() {
        usersServiceContainer.start();

        String userCreationApiPath = String.format(
            "http://localhost:%s/users",
            usersServiceContainer.getMappedPort(USERS_SERVICE_PORT));
        UserCreationRequest userCreationRequest = new UserCreationRequest("John", "Doe", "12345678", "test@example.com");

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<UserCreationResponse> userCreationResponse = restTemplate.postForEntity(
            userCreationApiPath,
            userCreationRequest,
            UserCreationResponse.class
        );
        createdTestUserId = userCreationResponse.getBody().getUserId();
    }
}