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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final TaskMapper taskMapper;
  private final com.taskTracker.taskTracker.event.TaskEventPublisher taskEventPublisher;

  public TaskServiceImpl(
      TaskRepository taskRepository,
      UserRepository userRepository,
      TaskMapper taskMapper,
      com.taskTracker.taskTracker.event.TaskEventPublisher taskEventPublisher) {
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.taskMapper = taskMapper;
    this.taskEventPublisher = taskEventPublisher;
  }

  @Override
  public TaskResponse createTask(TaskRequest request, Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

    Task task = taskMapper.toEntity(request);
    task.setOwner(user);

    Task savedTask = taskRepository.save(task);
    TaskResponse response = taskMapper.toResponse(savedTask);
    taskEventPublisher.publishEvent(
        com.taskTracker.taskTracker.event.TaskEventType.CREATED, response);
    return response;
  }

  @Override
  public PageResponse<TaskResponse> getTasks(
      com.taskTracker.taskTracker.entity.TaskStatus status,
      Long ownerId,
      Pageable pageable,
      User currentUser) {

    Specification<Task> spec = Specification.where(null);

    // 1. Enforce ownership for non-admins
    if (currentUser.getRole() != com.taskTracker.taskTracker.entity.Role.ADMIN) {
      spec =
          spec.and((root, query, cb) -> cb.equal(root.get("owner").get("id"), currentUser.getId()));
    } else if (ownerId != null) {
      // Admin can filter by ownerId
      spec = spec.and((root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId));
    }

    // 2. Filter by status
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }

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

    TaskResponse response = taskMapper.toResponse(updatedTask);
    taskEventPublisher.publishEvent(
        com.taskTracker.taskTracker.event.TaskEventType.UPDATED, response);
    return response;
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

    TaskResponse response = taskMapper.toResponse(task);
    taskRepository.delete(task);
    taskEventPublisher.publishEvent(
        com.taskTracker.taskTracker.event.TaskEventType.DELETED, response);
  }
}
