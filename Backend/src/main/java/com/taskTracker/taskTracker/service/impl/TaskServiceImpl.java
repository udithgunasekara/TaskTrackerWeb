package com.taskTracker.taskTracker.service.impl;

import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.PageResponse;
import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.entity.Task;
import com.taskTracker.taskTracker.entity.User;
import com.taskTracker.taskTracker.mapper.TaskMapper;
import com.taskTracker.taskTracker.repository.TaskRepository;
import com.taskTracker.taskTracker.repository.UserRepository;
import com.taskTracker.taskTracker.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final TaskMapper taskMapper;

  public TaskServiceImpl(
      TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.taskMapper = taskMapper;
  }

  @Override
  public TaskResponse createTask(TaskRequest request, Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    Task task = taskMapper.toEntity(request);
    task.setOwner(user);

    Task savedTask = taskRepository.save(task);
    return taskMapper.toResponse(savedTask);
  }

  @Override
  public PageResponse<TaskResponse> getTasks(Long userId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("owner").get("id"), userId);

    Page<Task> taskPage = taskRepository.findAll(spec, pageable);
    Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);

    return PageResponse.from(responsePage);
  }

  @Override
  public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId) {
    Task task =
        taskRepository
            .findByIdAndOwnerId(taskId, userId)
            .orElseThrow(
                () ->
                    new com.taskTracker.taskTracker.exception.ResourceNotFoundException(
                        "Task", taskId));

    taskMapper.updateEntity(request, task);
    Task updatedTask = taskRepository.save(task);

    return taskMapper.toResponse(updatedTask);
  }

  @Override
  public void deleteTask(Long taskId, Long userId) {
    Task task =
        taskRepository
            .findByIdAndOwnerId(taskId, userId)
            .orElseThrow(
                () ->
                    new com.taskTracker.taskTracker.exception.ResourceNotFoundException(
                        "Task", taskId));

    taskRepository.delete(task);
  }
}
