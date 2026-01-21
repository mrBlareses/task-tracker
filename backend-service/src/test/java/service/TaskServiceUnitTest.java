package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.authority.RoleType;
import org.mushroom.entity.Role;
import org.mushroom.entity.Task;
import org.mushroom.entity.User;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.ChangeTaskStatusRequest;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.TaskService;
import org.mushroom.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private Clock clock;

    private UUID taskId;
    private UUID userId;
    private User currentUser;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        currentUser = User.builder()
                .id(userId)
                .email("user@test.com")
                .build();

        currentUser.getRoles()
                .add(Role.builder().name(RoleType.USER).user(currentUser).build());

        when(userService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void updateTask_ShouldThrowNotFound_WhenTaskIsNotMine() {
        when(taskRepository.findByIdAndUserIdWithLock(taskId, userId))
                .thenReturn(Optional.empty());

        TaskUpdateRequest request = new TaskUpdateRequest().title("hack");

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.updateTask(taskId, request)
        );

        assertThat(ex.getMessage())
                .contains("Task not found")
                .contains(taskId.toString());
    }

    @Test
    void deleteTask_ShouldThrowNotFound_WhenTaskIsNotMine() {
        when(taskRepository.findByIdAndUserIdWithLock(taskId, userId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.deleteTask(taskId)
        );

        assertThat(ex.getMessage()).contains(taskId.toString());
    }

    @Test
    void getTaskById_ShouldThrowNotFound_WhenTaskIsNotMine() {
        when(taskRepository.findByIdAndUserId(taskId, userId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.getTaskById(taskId)
        );

        assertThat(ex.getMessage()).contains(taskId.toString());
    }


    @Test
    void createTask_ShouldHandleException_WhenRepositoryFails() {
        TaskCreateRequest request = new TaskCreateRequest().title("test");
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        Task entity = Task.builder().build();
        when(taskMapper.toEntity(request)).thenReturn(entity);

        when(taskRepository.save(any(Task.class)))
                .thenThrow(new DataIntegrityViolationException("db error"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> taskService.createTask(request)
        );
    }

    @Test
    void changeTaskStatus_ShouldThrowNotFound_WhenTaskIsNotMine() {
        when(taskRepository.findByIdAndUserIdWithLock(taskId, userId))
                .thenReturn(Optional.empty());

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest()
                .status(ChangeTaskStatusRequest.StatusEnum.DONE);

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.changeTaskStatus(taskId, request)
        );

        assertThat(ex.getMessage()).contains(taskId.toString());
    }

    @Test
    void changeTaskStatus_ShouldThrowNullPointer_WhenStatusNull() {
        Task task = Task.builder()
                .id(taskId)
                .user(currentUser)
                .build();

        when(taskRepository.findByIdAndUserIdWithLock(taskId, userId))
                .thenReturn(Optional.of(task));

        ChangeTaskStatusRequest request = mock(ChangeTaskStatusRequest.class);
        when(request.getStatus()).thenReturn(null);

        assertThrows(
                NullPointerException.class,
                () -> taskService.changeTaskStatus(taskId, request)
        );
    }

    @Test
    void createTask_ShouldUseUtcClock() {
        Instant fixedInstant = Instant.parse("2025-06-01T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        TaskService serviceWithUtcClock =
                new TaskService(taskRepository, fixedClock, taskMapper, userService);

        TaskCreateRequest request = new TaskCreateRequest().title("UTC test");

        Task entity = Task.builder().build();
        Task saved = Task.builder().build();

        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(taskMapper.toEntity(request)).thenReturn(entity);
        when(taskRepository.save(any())).thenReturn(saved);
        when(taskMapper.toResponse(saved)).thenReturn(new TaskResponse());

        serviceWithUtcClock.createTask(request);

        assertThat(entity.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(entity.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }
}