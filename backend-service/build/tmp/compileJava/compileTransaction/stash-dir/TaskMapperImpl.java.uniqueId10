package org.mushroom.mapper;

import javax.annotation.processing.Generated;
import org.mushroom.entity.Task;
import org.mushroom.enums.TaskStatus;
import org.mushroom.model.task.TaskCreateRequest;
import org.mushroom.model.task.TaskResponse;
import org.mushroom.model.task.TaskUpdateRequest;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.6.2, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 21.0.9 (Amazon.com Inc.)"
)
@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public Task toEntity(TaskCreateRequest dto) {
        if ( dto == null ) {
            return null;
        }

        Task.TaskBuilder task = Task.builder();

        task.title( dto.getTitle() );
        task.description( dto.getDescription() );

        task.status( TaskStatus.TODO );

        return task.build();
    }

    @Override
    public void updateEntity(Task task, TaskUpdateRequest dto) {
        if ( dto == null ) {
            return;
        }

        task.setStatus( mapStatusFromDto( dto.getStatus() ) );
        task.setTitle( dto.getTitle() );
        task.setDescription( dto.getDescription() );
    }

    @Override
    public TaskResponse toResponse(Task task) {
        if ( task == null ) {
            return null;
        }

        TaskResponse taskResponse = new TaskResponse();

        taskResponse.setId( task.getId() );
        taskResponse.setTitle( task.getTitle() );
        taskResponse.setDescription( task.getDescription() );
        taskResponse.setStatus( taskStatusToStatusEnum( task.getStatus() ) );
        taskResponse.setCreatedAt( task.getCreatedAt() );
        taskResponse.setUpdatedAt( task.getUpdatedAt() );

        return taskResponse;
    }

    protected TaskResponse.StatusEnum taskStatusToStatusEnum(TaskStatus taskStatus) {
        if ( taskStatus == null ) {
            return null;
        }

        TaskResponse.StatusEnum statusEnum;

        switch ( taskStatus ) {
            case TODO: statusEnum = TaskResponse.StatusEnum.TODO;
            break;
            case IN_PROGRESS: statusEnum = TaskResponse.StatusEnum.IN_PROGRESS;
            break;
            case DONE: statusEnum = TaskResponse.StatusEnum.DONE;
            break;
            default: throw new IllegalArgumentException( "Unexpected enum constant: " + taskStatus );
        }

        return statusEnum;
    }
}
