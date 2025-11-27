package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.BackendApplication;
import org.mushroom.authority.RoleType;
import org.mushroom.entity.Role;
import org.mushroom.entity.User;
import org.mushroom.model.UserRegistrationRequest;
import org.mushroom.model.UserUpdateRequest;
import org.mushroom.repository.UserRepository;
import org.mushroom.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class UserControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    private String userToken;
    private String otherUserToken;
    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = createAndSaveUser("user@test.com", "123456", "User", "Test");
        userId = user.getId();
        userToken = jwtService.generateToken(user);

        User otherUser = createAndSaveUser("other@test.com", "123456", "Other", "User");
        otherUserId = otherUser.getId();
        otherUserToken = jwtService.generateToken(otherUser);

        assert userToken != null && !userToken.isBlank() : "userToken is null!";
        assert otherUserToken != null && !otherUserToken.isBlank() : "otherUserToken is null!";
    }

    @Test
    @DisplayName("POST /api/v1/users/register — должен вернуть 409 при повторном email")
    void shouldReturnConflict_OnDuplicateEmail() throws Exception {
        createAndSaveUser("mrblare@mail.ru", "1234567", "Name", "Doe");

        UserRegistrationRequest request = new UserRegistrationRequest()
                .email("mrblare@mail.ru")
                .password("1234567")
                .firstName("Name")
                .lastName("Doe");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — должен вернуть 200 и пользователя")
    void shouldGetUserById() throws Exception {
        User saved = createAndSaveUser("mrblare@mail.ru", "1234567", "Name", "Doe");
        String token = jwtService.generateToken(saved);

        mockMvc.perform(get("/api/v1/users/{id}", saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mrblare@mail.ru"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} — должен вернуть 404, если пользователь не найден")
    void shouldReturnNotFound_ForMissingUser() throws Exception {
        UUID missingId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/users/{id}", missingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User with id '" + missingId + "' not found")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} — пользователь может обновить только свой профиль")
    void shouldReturnOk_UpdateOwnProfile() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest()
                .firstName("NewName")
                .lastName("NewLastName");

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewName"))
                .andExpect(jsonPath("$.lastName").value("NewLastName"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} — пользователь НЕ может обновить чужой профиль → 403")
    void shouldReturnForbidden_UpdateOthersProfile() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest().firstName("Hacked");

        mockMvc.perform(put("/api/v1/users/{id}", otherUserId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only update your profile"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} — пользователь может удалить только свой аккаунт")
    void shouldReturnNoContent_DeleteOwnAccount() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} — пользователь НЕ может удалить чужой аккаунт → 403")
    void shouldReturnForbidden_DeleteOthersAccount() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", otherUserId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only delete your account"));
    }

    private User createAndSaveUser(String email, String password, String firstName, String lastName) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstname(firstName)
                .lastname(lastName)
                .createdAt(OffsetDateTime.now(clock))
                .updatedAt(OffsetDateTime.now(clock))
                .build();

        Role role = Role.builder()
                .name(RoleType.USER)
                .user(user)
                .build();
        user.getRoles().add(role);

        return userRepository.save(user);
    }
}
