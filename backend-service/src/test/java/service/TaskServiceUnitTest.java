package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.entity.Task;
import org.mushroom.entity.User;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.TaskService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TaskServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private Clock clock;
    private UUID taskId;
    private UUID userId;
    private User currentUser;
    private Instant fixedInstant;
    private ZoneId fixedZone;
    private OffsetDateTime fixedNow;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        currentUser = User.builder()
                .id(userId)
                .email("user@test.com")
                .build();

        fixedInstant = Instant.parse("2026-01-23T10:15:30Z");
        fixedZone = ZoneOffset.ofHours(3);
        fixedNow = OffsetDateTime.ofInstant(fixedInstant, fixedZone);
    }

    private void mockTimezone() {
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(fixedZone);
    }

    @Test
    @DisplayName("getTaskById — возвращает задачу если она существует")
    void getTaskById_ShouldReturnTask() {
        Task task = Task.builder().id(taskId).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Task actual = taskService.getTaskById(taskId);

        assertThat(actual).isSameAs(task);
    }

    @Test
    @DisplayName("getTaskById — бросает NotFoundException если задачи нет")
    void getTaskById_ShouldThrowNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.getTaskById(taskId)
        );

        assertThat(ex.getMessage()).contains(taskId.toString());
    }

    @Test
    @DisplayName("updateTask — обновляет задачу и updatedAt")
    void updateTask_ShouldUpdateTask() {
        mockTimezone();

        TaskUpdateRequest request = new TaskUpdateRequest()
                .title("new title")
                .description("new desc");

        Task returned = Task.builder()
                .id(taskId)
                .title("new title")
                .description("new desc")
                .updatedAt(fixedNow)
                .build();

        TaskResponse response = new TaskResponse().id(taskId);

        when(taskRepository.update(
                eq(taskId),
                eq("new title"),
                eq("new desc"),
                isNull(),
                eq(fixedNow)
        )).thenReturn(Optional.of(returned));

        when(taskMapper.toResponse(returned)).thenReturn(response);

        TaskResponse actual = taskService.updateTask(taskId, request);

        assertThat(actual).isEqualTo(response);
    }

    @Test
    @DisplayName("updateTask — бросает NotFoundException если задачи нет")
    void updateTask_ShouldThrowNotFound() {
        mockTimezone();

        TaskUpdateRequest request = new TaskUpdateRequest().title("x");

        when(taskRepository.update(
                eq(taskId),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(
                NotFoundException.class,
                () -> taskService.updateTask(taskId, request)
        );

        assertThat(ex.getMessage()).contains(taskId.toString());
    }
}