package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.Task;
import org.mushroom.entity.TaskStatus;
import org.mushroom.entity.User;
import org.mushroom.exception.NotFoundException;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.ChangeTaskStatusRequest;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.repository.TaskRepository;
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
        Task task = getTaskByIdOrThrow(id);

        TaskStatus newStatus = TaskStatus.valueOf(request.getStatus().getValue());
        task.setStatus(newStatus);
        task.setUpdatedAt(OffsetDateTime.now(clock));

        Task saved = taskRepository.save(task);
        return taskMapper.toResponse(saved);
    }

    public void deleteTask(UUID id) {
        taskRepository.deleteById(id);
    }

    public Task getTaskById(UUID id) {
        return getTaskByIdOrThrow(id);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskUpdateRequest request) {
        Task existing = getTaskByIdOrThrow(id);
        taskMapper.updateEntity(existing, request);
        existing.setUpdatedAt(OffsetDateTime.now(clock));
        Task updatedTask = taskRepository.save(existing);
        return taskMapper.toResponse(updatedTask);
    }

    private Task getTaskByIdOrThrow(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }
}
