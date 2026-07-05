package com.taskTracker.taskTracker.service;

import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.PageResponse;
import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.entity.TaskStatus;
import org.springframework.data.domain.Pageable;

public interface TaskService {
  TaskResponse createTask(TaskRequest request, Long userId);

  PageResponse<TaskResponse> getTasks(
      TaskStatus status,
      Long ownerId,
      Pageable pageable,
      com.taskTracker.taskTracker.entity.User currentUser);

  TaskResponse updateTask(Long taskId, TaskRequest request, Long userId);

  void deleteTask(Long taskId, Long userId);
}
