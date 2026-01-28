package org.mushroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mushroom.api.task.TasksApi;
import org.mushroom.entity.Task;
import org.mushroom.mapper.TaskMapper;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskUpdateRequest;
import org.mushroom.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TaskController implements TasksApi {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @Override
    public ResponseEntity<TaskResponse> getTaskById(UUID id) {
        Task task = taskService.getTaskById(id);
        return ResponseEntity.ok(taskMapper.toResponse(task));
    }

    @Override
    public ResponseEntity<TaskResponse> createTask(@Valid TaskCreateRequest taskCreateRequest) {
        TaskResponse response = taskService.createTask(taskCreateRequest);
        return ResponseEntity
                .created(URI.create("/api/v1/tasks/" + response.getId()))
                .body(response);
    }

    @Override
    public ResponseEntity<TaskResponse> updateTask(UUID id, @Valid TaskUpdateRequest taskUpdateRequest) {
        TaskResponse response = taskService.updateTask(id, taskUpdateRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteTask(UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}

