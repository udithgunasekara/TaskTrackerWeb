package com.taskTracker.taskTracker.task.service;

import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.task.payload.request.TaskRequest;
import com.taskTracker.taskTracker.task.payload.response.PageResponse;
import com.taskTracker.taskTracker.task.payload.response.TaskResponse;
import com.taskTracker.taskTracker.task.type.TaskStatus;
import org.springframework.data.domain.Pageable;

public interface TaskService {
  TaskResponse createTask(TaskRequest request, Long userId);

  PageResponse<TaskResponse> getTasks(
      TaskStatus status, Long ownerId, Pageable pageable, User currentUser);

  TaskResponse updateTask(Long taskId, TaskRequest request, Long userId);

  void deleteTask(Long taskId, Long userId);
}
