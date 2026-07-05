package com.taskTracker.taskTracker.mapper;

import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    uses = UserMapper.class,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TaskMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Task toEntity(TaskRequest request);

  TaskResponse toResponse(Task task);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntity(TaskRequest request, @MappingTarget Task task);
}
