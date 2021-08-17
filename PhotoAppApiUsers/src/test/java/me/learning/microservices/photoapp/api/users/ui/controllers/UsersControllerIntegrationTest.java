package me.learning.microservices.photoapp.api.users.ui.controllers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static me.learning.microservices.photoapp.api.users.ui.controllers.utils.UserResponseUtils.createdUserResponseEqualsWithoutUserId;
import static me.learning.microservices.photoapp.api.users.ui.controllers.utils.UserResponseUtils.userResponseEqualsWithoutAlbums;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.mkammerer.wiremock.WireMockExtension;
import java.util.List;
import java.util.UUID;
import me.learning.microservices.photoapp.api.users.data.user.User;
import me.learning.microservices.photoapp.api.users.data.user.UsersRepository;
import me.learning.microservices.photoapp.api.users.ui.model.AlbumResponse;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserRequest;
import me.learning.microservices.photoapp.api.users.ui.model.CreateUserResponse;
import me.learning.microservices.photoapp.api.users.ui.model.UserResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({ "test", "security-disabled" })
class UsersControllerIntegrationTest {

    @RegisterExtension
    static WireMockExtension albumsServiceServer = new WireMockExtension(9561);

    private static ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository usersRepository;

    @BeforeAll
    static void setUpObjectMapper() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void get_status_message() throws Exception {
        mockMvc.perform(get("/users/status/check")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(not(blankOrNullString())));
    }

    @Test
    void get_user_without_albums_ok() throws Exception {
        User createdUser = createTestUser();

        mockMvc.perform(get("/users/" + createdUser.getUserId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                UserResponse userResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), UserResponse.class);
                assertThat(userResp)
                    .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
            });
    }

    @Test
    void get_user_without_albums_not_found() throws Exception {
        createTestUser();

        mockMvc.perform(get("/users/2")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void get_user_with_albums_ok() throws Exception {
        User createdUser = createTestUser();

        var albumResponse1 = new AlbumResponse();
        albumResponse1.setUserId(createdUser.getUserId());
        albumResponse1.setName("Test Album 1");
        albumResponse1.setAlbumId("123456-4687");
        var albumResponse2 = new AlbumResponse();
        albumResponse2.setUserId(createdUser.getUserId());
        albumResponse2.setName("Test Album 2");
        albumResponse2.setAlbumId("345689-4687");
        var albumResponse3 = new AlbumResponse();
        albumResponse3.setUserId(createdUser.getUserId());
        albumResponse3.setName("Test Album 3");
        albumResponse3.setAlbumId("587857-4687");
        var albumResponses = List.of(albumResponse1, albumResponse2, albumResponse3);

        albumsServiceServer.stubFor(
            WireMock.get(urlPathEqualTo("/users/" + createdUser.getUserId() + "/albums"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(albumResponses)))
        );

        mockMvc.perform(get("/users/" + createdUser.getUserId())
            .queryParam("withAlbums", "true")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                UserResponse userResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), UserResponse.class);
                assertThat(userResp)
                    .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
                assertThat(userResp.getAlbums())
                    .isNotEmpty()
                    .allMatch(albumResponse -> createdUser.getUserId().equals(albumResponse.getUserId()));
            });
    }

    @Test
    void get_user_with_albums_external_service_failure() throws Exception {
        User createdUser = createTestUser();

        albumsServiceServer.stubFor(
            WireMock.get(urlPathEqualTo("/users/" + createdUser.getUserId() + "/albums"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        );

        mockMvc.perform(get("/users/" + createdUser.getUserId())
            .queryParam("withAlbums", "true")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> {
                UserResponse userResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), UserResponse.class);
                assertThat(userResp)
                    .matches(userResponse -> userResponseEqualsWithoutAlbums(createdUser, userResponse));
                assertThat(userResp.getAlbums())
                    .isEmpty();
            });
    }

    @Test
    void get_user_with_albums_not_found() throws Exception {
        createTestUser();

        mockMvc.perform(get("/users/2")
            .queryParam("withAlbums", "true")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_user_ok() throws Exception {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail("test@example.com");
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setPassword("12345678");

        CreateUserResponse expectedCreatedUserResponse = new CreateUserResponse();
        expectedCreatedUserResponse.setEmail("test@example.com");
        expectedCreatedUserResponse.setFirstName("John");
        expectedCreatedUserResponse.setLastName("Doe");

        mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createUserRequest)))
            .andExpect(status().isCreated())
            .andExpect(mvcResult -> {
                CreateUserResponse creationResp = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), CreateUserResponse.class);
                assertThat(creationResp)
                    .matches(createdResponse -> createdUserResponseEqualsWithoutUserId(expectedCreatedUserResponse, createdResponse));
            });
    }

    @Test
    void create_user_already_exists() throws Exception {
        User alreadyExistsUser = createTestUser();

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail(alreadyExistsUser.getEmail());
        createUserRequest.setFirstName("Sally");
        createUserRequest.setLastName("Smith");
        createUserRequest.setPassword("12345678");

        mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createUserRequest)))
            .andExpect(status().isConflict());
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
}