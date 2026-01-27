package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.Task;
import org.mushroom.entity.User;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.ChangeTaskStatusRequest;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.repository.TaskRepository;
import org.mushroom.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final Clock clock;
    private final TaskMapper taskMapper;
    private final UserService userService;

    public TaskResponse createTask(TaskCreateRequest request) {
        User currentUser = userService.getCurrentUser();
        OffsetDateTime now = OffsetDateTime.now(clock);

        Task task = taskMapper.toEntity(request);
        task.setUser(currentUser);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse changeTaskStatus(UUID id, ChangeTaskStatusRequest request) {
        String status = request.getStatus().getValue();

        Task updated = taskRepository.update(
                null,
                null,
                status,
                OffsetDateTime.now(clock)
        ).orElseThrow(() -> new NotFoundException("Task not found: " + id));

        return taskMapper.toResponse(updated);
    }

    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }

    public Task getTaskById(UUID id) {
        return getExistingTask(id);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskUpdateRequest request) {
        String title = ValidationUtils
                .trimOrBadRequestIfBlank(request.getTitle(), "title");

        String description = ValidationUtils
                .trimOrBadRequestIfBlank(request.getDescription(), "description");

        String status = request.getStatus() != null ? request.getStatus().name() : null;

        Task updated = taskRepository.update(
                title,
                description,
                status,
                OffsetDateTime.now(clock)
        ).orElseThrow(() -> new NotFoundException("Task not found: " + id));
        return taskMapper.toResponse(updated);
    }

    private Task getExistingTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Task was validated but not found"));
    }
}
