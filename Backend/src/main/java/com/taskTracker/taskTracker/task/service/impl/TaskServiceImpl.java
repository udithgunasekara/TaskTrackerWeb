package com.taskTracker.taskTracker.task.service.impl;

import com.taskTracker.taskTracker.common.exception.EntityNotFoundException;
import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.common.repository.UserDao;
import com.taskTracker.taskTracker.common.type.Role;
import com.taskTracker.taskTracker.task.constant.TaskMessageConstant;
import com.taskTracker.taskTracker.task.mapper.TaskMapper;
import com.taskTracker.taskTracker.task.model.Task;
import com.taskTracker.taskTracker.task.payload.request.TaskRequest;
import com.taskTracker.taskTracker.task.payload.response.PageResponse;
import com.taskTracker.taskTracker.task.payload.response.TaskResponse;
import com.taskTracker.taskTracker.task.repository.TaskDao;
import com.taskTracker.taskTracker.task.service.TaskService;
import com.taskTracker.taskTracker.task.type.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskServiceImpl implements TaskService {

  private final TaskDao taskDao;
  private final UserDao userDao;
  private final TaskMapper taskMapper;

  public TaskServiceImpl(TaskDao taskDao, UserDao userDao, TaskMapper taskMapper) {
    this.taskDao = taskDao;
    this.userDao = userDao;
    this.taskMapper = taskMapper;
  }

  @Override
  @Transactional
  public TaskResponse createTask(TaskRequest request, Long userId) {
    User user =
        userDao
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(TaskMessageConstant.TASK_NOT_FOUND));

    Task task = taskMapper.toEntity(request);
    task.setOwner(user);

    Task savedTask = taskDao.save(task);
    return taskMapper.toResponse(savedTask);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<TaskResponse> getTasks(
      TaskStatus status, Long ownerId, Pageable pageable, User currentUser) {

    Specification<Task> spec = Specification.where(null);

    // 1. Enforce ownership for non-admins
    if (currentUser.getRole() != Role.ADMIN) {
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

    Page<Task> taskPage = taskDao.findAll(spec, pageable);
    Page<TaskResponse> responsePage = taskPage.map(taskMapper::toResponse);

    return PageResponse.from(responsePage);
  }

  @Override
  @Transactional
  public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId) {
    Task task =
        taskDao
            .findByIdAndOwnerId(taskId, userId)
            .orElseThrow(() -> new EntityNotFoundException(TaskMessageConstant.TASK_NOT_FOUND));

    taskMapper.updateEntity(request, task);
    Task updatedTask = taskDao.save(task);

    return taskMapper.toResponse(updatedTask);
  }

  @Override
  public void deleteTask(Long taskId, Long userId) {
    Task task =
        taskDao
            .findByIdAndOwnerId(taskId, userId)
            .orElseThrow(() -> new EntityNotFoundException(TaskMessageConstant.TASK_NOT_FOUND));

    taskDao.delete(task);
  }
}
