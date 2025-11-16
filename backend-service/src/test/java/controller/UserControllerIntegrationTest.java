package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.BackendApplication;
import org.mushroom.entity.User;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Testcontainers
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/users — должен вернуть 409 при повторном email")
    void shouldReturnConflictOnDuplicateEmail() throws Exception {
        createUser("mrblare@mail.ru", "1234567", "Name", "Doe");

        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("mrblare@mail.ru")
                .password("1234567")
                .firstName("Name")
                .lastName("Doe");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — должен вернуть 200 и пользователя")
    void shouldGetUserById() throws Exception {
        User saved = saveUser("mrblare@mail.ru", "1234567", "Name", "Doe");

        mockMvc.perform(get("/api/v1/users/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mrblare@mail.ru"))
                .andExpect(jsonPath("$.firstName").value("Name"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — должен вернуть 404, если пользователь не найден")
    void shouldReturnNotFoundForMissingUser() throws Exception {
        UUID missingId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/users/{id}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User with id '" + missingId + "' not found")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} — должен обновлять имя пользователя")
    void shouldUpdateUser() throws Exception {
        User saved = saveUser("mrblare@mail.ru", "1234567", "Name", "Doe");

        UserUpdateRequest updateRequest = new UserUpdateRequest()
                .firstName("NewName")
                .lastName("Updated");

        mockMvc.perform(put("/api/v1/users/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewName"))
                .andExpect(jsonPath("$.lastName").value("Updated"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} — должен удалять пользователя")
    void shouldDeleteUser() throws Exception {
        User saved = saveUser("mrblare@mail.ru", "1234567", "Name", "Doe");

        mockMvc.perform(delete("/api/v1/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());
    }

    private User createUser(String email, String password, String firstName, String lastName) {
        return User.builder()
                .email(email)
                .password(password)
                .firstname(firstName)
                .lastname(lastName)
                .createdAt(OffsetDateTime.now(clock))
                .updatedAt(OffsetDateTime.now(clock))
                .build();
    }

    private User saveUser(String email, String password, String firstName, String lastName) {
        return userRepository.save(createUser(email, password, firstName, lastName));
    }
}
