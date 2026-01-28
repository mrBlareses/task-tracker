package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskUpdateRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
class TaskControllerIntegrationTest extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

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
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User user = createAndSaveUser("user@test.com");
        User other = createAndSaveUser("other@test.com");

        userId = user.getId();
        otherUserId = other.getId();

        userToken = jwtService.generateToken(user);
        otherUserToken = jwtService.generateToken(other);
    }

    @Test
    @DisplayName("POST /api/v1/tasks — должен создать задачу")
    void shouldCreateTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest()
                .title("Новая задача")
                .description("Тестовое описание");

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Новая задача"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks — 400 если title пустой")
    void shouldReturnBadRequest_WhenTitleBlank() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest()
                .title("   ")
                .description("desc");

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} — возвращает задачу по id")
    void shouldGetTaskById_EvenIfNotOwner() throws Exception {
        Task task = createAndSaveTask("Чужая задача", otherUserId);

        mockMvc.perform(get("/api/v1/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Чужая задача"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} — 404 если задачи нет")
    void shouldReturnNotFound_WhenTaskNotExists() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/tasks/{id}", missingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} — обновляет задачу по id")
    void shouldUpdateTask_EvenIfNotOwner() throws Exception {
        Task task = createAndSaveTask("Старая задача", otherUserId);

        TaskUpdateRequest request = new TaskUpdateRequest()
                .title("Новое название")
                .description("Новое описание")
                .status(TaskUpdateRequest.StatusEnum.DONE);

        mockMvc.perform(put("/api/v1/tasks/{id}", task.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId().toString()))
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.description").value("Новое описание"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} — удаляет задачу по id")
    void shouldDeleteTask_EvenIfNotOwner() throws Exception {
        Task task = createAndSaveTask("Удаляемая", otherUserId);

        mockMvc.perform(delete("/api/v1/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.findById(task.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} — 204 даже если задачи нет (deleteById не бросает ошибку)")
    void shouldReturnNoContent_WhenDeletingMissingTask() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/tasks/{id}", missingId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .firstname("Test")
                .lastname("User")
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

    private Task createAndSaveTask(String title, UUID ownerId) {
        User owner = userRepository.findById(ownerId).orElseThrow();

        Task task = Task.builder()
                .title(title)
                .description("desc")
                .status(TaskStatus.TODO)
                .user(owner)
                .createdAt(OffsetDateTime.now(clock))
                .updatedAt(OffsetDateTime.now(clock))
                .build();

        return taskRepository.save(task);
    }
}

