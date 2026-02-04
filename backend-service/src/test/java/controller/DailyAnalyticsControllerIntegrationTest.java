package controller;

import config.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.BackendApplication;
import org.mushroom.authority.RoleType;
import org.mushroom.entity.Role;
import org.mushroom.entity.Task;
import org.mushroom.entity.TaskStatus;
import org.mushroom.entity.User;
import org.mushroom.repository.TaskRepository;
import org.mushroom.repository.UserRepository;
import org.mushroom.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
class DailyAnalyticsControllerIntegrationTest extends PostgresTestContainer {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private Clock clock;

    private String serviceToken;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        User service = createAndSaveUser("service@test.com", RoleType.SERVICE);
        serviceToken = jwtService.generateToken(service);

        User userA = createAndSaveUser("a@test.com", RoleType.USER);
        User userB = createAndSaveUser("b@test.com", RoleType.USER);

        userAId = userA.getId();
        userBId = userB.getId();
    }

    @Test
    @DisplayName("GET /api/v1/service/daily-analytics — возвращает summaries с корректными pending/completed")
    void shouldReturnDailyAnalytics() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(clock);
        ZoneOffset offset = now.getOffset();
        OffsetDateTime to = now.toLocalDate().atStartOfDay().atOffset(offset);
        OffsetDateTime from = to.minusDays(1);

        createAndSaveTask("A todo", userAId, TaskStatus.TODO, from.plusHours(1));
        createAndSaveTask("A in progress", userAId, TaskStatus.IN_PROGRESS, from.plusHours(2));
        createAndSaveTask("A done in window", userAId, TaskStatus.DONE, from.plusHours(3));
        createAndSaveTask("A done outside", userAId, TaskStatus.DONE, from.minusHours(1));

        createAndSaveTask("B done 1", userBId, TaskStatus.DONE, from.plusHours(4));
        createAndSaveTask("B done 2", userBId, TaskStatus.DONE, from.plusHours(5));

        mockMvc.perform(get("/api/v1/service/daily-analytics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.from").exists())
                .andExpect(jsonPath("$.to").exists())
                .andExpect(jsonPath("$.summaries").isArray())

                .andExpect(jsonPath("$.summaries[?(@.email=='a@test.com')].pendingCount").value(2))
                .andExpect(jsonPath("$.summaries[?(@.email=='a@test.com')].completedCount").value(1))

                .andExpect(jsonPath("$.summaries[?(@.email=='b@test.com')].pendingCount").value(0))
                .andExpect(jsonPath("$.summaries[?(@.email=='b@test.com')].completedCount").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/service/daily-analytics — 401 без токена")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/service/daily-analytics"))
                .andExpect(status().isUnauthorized());
    }

    private User createAndSaveUser(String email, RoleType roleType) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .firstname("Test")
                .lastname("User")
                .createdAt(OffsetDateTime.now(clock))
                .updatedAt(OffsetDateTime.now(clock))
                .build();

        Role role = Role.builder()
                .name(roleType)
                .user(user)
                .build();

        user.getRoles().add(role);

        return userRepository.save(user);
    }

    private Task createAndSaveTask(String title, UUID ownerId, TaskStatus status, OffsetDateTime updatedAt) {
        User owner = userRepository.findById(ownerId).orElseThrow();

        Task task = new Task();
        task.setTitle(title);
        task.setDescription("desc");
        task.setStatus(status);
        task.setUser(owner);
        task.setCreatedAt(updatedAt.minusMinutes(10));
        task.setUpdatedAt(updatedAt);

        return taskRepository.save(task);
    }
}
