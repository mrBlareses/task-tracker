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

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        User currentUser = getCurrentUser();
        OffsetDateTime now = OffsetDateTime.now(clock);

        Task task = taskMapper.toEntity(request);
        task.setUser(currentUser);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse changeTaskStatus(UUID id, ChangeTaskStatusRequest request) {
        Task task = getOwnTaskByIdWithLock(id);

        TaskStatus newStatus = TaskStatus.valueOf(request.getStatus().getValue());
        task.setStatus(newStatus);
        task.setUpdatedAt(OffsetDateTime.now(clock));

        Task updatedTask = taskRepository.save(task);
        return taskMapper.toResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID id) {
        Task task = getOwnTaskByIdWithLock(id);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Task getTaskById(UUID id) {
        return getOwnTaskById(id);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskUpdateRequest request) {
        Task existing = getOwnTaskByIdWithLock(id);

        taskMapper.updateEntity(existing, request);
        existing.setUpdatedAt(OffsetDateTime.now(clock));

        Task updatedTask = taskRepository.save(existing);
        return taskMapper.toResponse(updatedTask);
    }

    private User getCurrentUser() {
        return userService.getCurrentUser();
    }

    private Task getOwnTaskById(UUID id) {
        User currentUser = getCurrentUser();
        return taskRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() ->
                        new NotFoundException("Task not found or access denied: " + id)
                );
    }

    private Task getOwnTaskByIdWithLock(UUID id) {
        UUID userId = getCurrentUser().getId();

        return taskRepository.findByIdAndUserIdWithLock(id, userId)
                .orElseThrow(() ->
                        new NotFoundException("Task not found or access denied: " + id)
                );
    }
}
