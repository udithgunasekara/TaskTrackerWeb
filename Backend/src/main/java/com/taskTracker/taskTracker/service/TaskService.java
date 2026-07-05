package com.taskTracker.taskTracker.service;

import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.PageResponse;
import com.taskTracker.taskTracker.dto.response.TaskResponse;

public interface TaskService {
  TaskResponse createTask(TaskRequest request, Long userId);

  PageResponse<TaskResponse> getTasks(Long userId, int page, int size);

  TaskResponse updateTask(Long taskId, TaskRequest request, Long userId);

  void deleteTask(Long taskId, Long userId);
}
