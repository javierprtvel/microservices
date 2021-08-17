package me.learning.microservices.photoapp.api.albums.ui.controllers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static me.learning.microservices.photoapp.api.albums.ui.controllers.utils.AlbumResponseUtils.albumResponseEquals;
import static me.learning.microservices.photoapp.api.albums.ui.controllers.utils.AlbumResponseUtils.albumResponseListTypeReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.user.Role;
import com.couchbase.client.java.manager.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.mkammerer.wiremock.WireMockExtension;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import me.learning.microservices.photoapp.api.albums.data.album.Album;
import me.learning.microservices.photoapp.api.albums.data.album.AlbumsRepository;
import me.learning.microservices.photoapp.api.albums.ui.model.AlbumResponse;
import me.learning.microservices.photoapp.api.albums.ui.model.CreateAlbumRequest;
import me.learning.microservices.photoapp.api.albums.ui.model.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Network;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles({ "test", "security-disabled" })
class AlbumsControllerIntegrationTest {

    private static final BucketDefinition albumsBucketDefinition;

    @Container
    private static final CouchbaseContainer couchbaseContainer;

    @RegisterExtension
    static WireMockExtension usersServiceServer = new WireMockExtension(9561);

    private static ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlbumsRepository albumsRepository;

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
        couchbaseContainer.start();
    }

    @DynamicPropertySource
    static void setUpPropertiesForExternalServices(DynamicPropertyRegistry registry) {
        couchbaseContainer.start();
        registry.add("spring.couchbase.connection-string", couchbaseContainer::getConnectionString);
    }

    @BeforeAll
    static void setUpAll() {
        objectMapper = new ObjectMapper();

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

    @AfterEach
    void deleteAllAlbums() {
        albumsRepository.deleteAll();
    }

    @Test
    void status_returns_status_message() throws Exception {
        mockMvc.perform(get("/albums/status/check")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(not(blankOrNullString())));
    }

    @Test
    void get_all_albums_ok() throws Exception {
        List<Album> createdAlbums = createAlbumsForUser("1");

        mockMvc.perform(get("/albums")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                var albumResponses = (List<AlbumResponse>) objectMapper.readValue(
                    mvcResult.getResponse().getContentAsByteArray(),
                    albumResponseListTypeReference()
                );
                assertThat(albumResponses)
                    .isNotEmpty()
                    // another test could have created albums concurrently
                    .hasSizeGreaterThanOrEqualTo(createdAlbums.size())
                    .filteredOn(albumResponse -> createdAlbums.stream()
                        .anyMatch(createdAlbum -> createdAlbum.getId().equals(albumResponse.getId()))
                    )
                    .allMatch(albumResponse -> createdAlbums.stream()
                        .anyMatch(createdAlbum -> albumResponseEquals(createdAlbum, albumResponse))
                    );
            });
    }

    @Test
    void get_album_ok() throws Exception {
        Album createdAlbum = createAlbum();

        mockMvc.perform(get("/albums/" + createdAlbum.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                AlbumResponse albumResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), AlbumResponse.class);
                assertThat(albumResp)
                    .matches(albumResponse -> albumResponseEquals(createdAlbum, albumResponse));
            });
    }

    @Test
    void get_album_not_found() throws Exception {
        createAlbum();

        mockMvc.perform(get("/albums/2")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void get_user_albums_ok() throws Exception {
        String userId = UUID.randomUUID().toString();
        List<Album> userAlbums = createAlbumsForUser(userId);

        mockMvc.perform(get("/users/" + userId + "/albums")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                var albumResponses = (List<AlbumResponse>) objectMapper.readValue(
                    mvcResult.getResponse().getContentAsByteArray(),
                    albumResponseListTypeReference()
                );
                assertThat(albumResponses)
                    .isNotEmpty()
                    .filteredOn(albumResponse -> userAlbums.stream().anyMatch(album -> album.getId().equals(albumResponse.getId())))
                    .isNotEmpty();
            });
    }

    @Test
    void get_user_albums_user_not_found() throws Exception {
        String userId = UUID.randomUUID().toString();
        createAlbumsForUser(userId);

        mockMvc.perform(get("/users/2/albums")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                var albumResponses = (List<AlbumResponse>) objectMapper.readValue(
                    mvcResult.getResponse().getContentAsByteArray(),
                    albumResponseListTypeReference()
                );
                assertThat(albumResponses)
                    .isEmpty();
            });
    }

    @Test
    void create_user_album_ok() throws Exception {
        String userId = UUID.randomUUID().toString();
        CreateAlbumRequest createAlbumRequest = new CreateAlbumRequest();
        createAlbumRequest.setUserId(userId);
        createAlbumRequest.setName("Test album");
        createAlbumRequest.setDescription("Album used for integration tests");

        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(userId);

        usersServiceServer.stubFor(
            WireMock.get(urlPathEqualTo("/users/" + userId))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(userResponse)))
        );

        mockMvc.perform(post("/users/" + userId + "/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createAlbumRequest)))
            .andExpect(status().isCreated())
            .andExpect(mvcResult -> {
                AlbumResponse albumResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), AlbumResponse.class);
                assertThat(albumResp)
                    .matches(albumResponse -> StringUtils.isNotBlank(albumResponse.getId())
                        && StringUtils.isNotBlank(albumResponse.getAlbumId())
                        && createAlbumRequest.getName().equals(albumResponse.getName())
                        && createAlbumRequest.getDescription().equals(albumResponse.getDescription())
                    );
            });
    }

    @Test
    void create_user_album_user_not_found() throws Exception {
        String userId = UUID.randomUUID().toString();
        CreateAlbumRequest createAlbumRequest = new CreateAlbumRequest();
        createAlbumRequest.setUserId(userId);
        createAlbumRequest.setName("Test album");
        createAlbumRequest.setDescription("Album used for integration tests");

        usersServiceServer.stubFor(
            WireMock.get(urlPathEqualTo("/users/" + userId))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.NOT_FOUND.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        );

        mockMvc.perform(post("/users/" + userId + "/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createAlbumRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_user_album_external_service_failure() throws Exception {
        String userId = UUID.randomUUID().toString();
        CreateAlbumRequest createAlbumRequest = new CreateAlbumRequest();
        createAlbumRequest.setUserId(userId);
        createAlbumRequest.setName("Test album");
        createAlbumRequest.setDescription("Album used for integration tests");

        usersServiceServer.stubFor(
            WireMock.get(urlPathEqualTo("/users/" + userId))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        );

        mockMvc.perform(post("/users/" + userId + "/albums")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createAlbumRequest)))
            .andExpect(status().isNotFound());
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
}