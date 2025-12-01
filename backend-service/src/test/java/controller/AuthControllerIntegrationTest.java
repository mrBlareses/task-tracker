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
import org.mushroom.model.LoginRequest;
import org.mushroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User testUser = User.builder()
                .email("auth@test.com")
                .password(passwordEncoder.encode("123456"))
                .firstname("Auth")
                .lastname("Test")
                .createdAt(OffsetDateTime.now(clock))
                .updatedAt(OffsetDateTime.now(clock))
                .build();

        Role role = Role.builder()
                .name(RoleType.USER)
                .user(testUser)
                .build();
        testUser.getRoles().add(role);

        userRepository.save(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — успешный логин → 200 + токен")
    void login_Success() throws Exception {
        LoginRequest request = new LoginRequest()
                .email("auth@test.com")
                .password("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400L));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — неверный пароль → 401")
    void login_InvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest()
                .email("auth@test.com")
                .password("wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed: Bad credentials"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — несуществующий пользователь → 401")
    void login_UserNotFound() throws Exception {
        LoginRequest request = new LoginRequest()
                .email("unknown@test.com")
                .password("123456");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication failed: User not found"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout — успешный логаут → 200")
    void logout_Success() throws Exception {
        String token = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest().email("auth@test.com").password("123456")
                        )))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + objectMapper.readTree(token).get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout — без токена → 401")
    void logout_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
