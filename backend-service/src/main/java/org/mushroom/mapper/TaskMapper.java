package org.mushroom.mapper;

import org.mapstruct.*;
import org.mushroom.entity.Task;
import org.mushroom.entity.TaskStatus;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", constant = "TODO")
    Task toEntity(TaskCreateRequest dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(
            target = "status",
            source = "status",
            qualifiedByName = "mapStatusFromDto"
    )
    void updateEntity(@MappingTarget Task task, TaskUpdateRequest dto);

    TaskResponse toResponse(Task task);

    @Named("mapStatusFromDto")
    default TaskStatus mapStatusFromDto(TaskUpdateRequest.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return TaskStatus.valueOf(status.name());
    }
}