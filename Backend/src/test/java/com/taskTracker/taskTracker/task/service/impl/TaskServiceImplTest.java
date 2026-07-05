package com.taskTracker.taskTracker.task.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.taskTracker.taskTracker.common.exception.EntityNotFoundException;
import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.common.repository.UserDao;
import com.taskTracker.taskTracker.common.type.Role;
import com.taskTracker.taskTracker.task.mapper.TaskMapper;
import com.taskTracker.taskTracker.task.model.Task;
import com.taskTracker.taskTracker.task.payload.request.TaskRequest;
import com.taskTracker.taskTracker.task.payload.response.PageResponse;
import com.taskTracker.taskTracker.task.payload.response.TaskResponse;
import com.taskTracker.taskTracker.task.repository.TaskDao;
import com.taskTracker.taskTracker.task.type.TaskStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

  @Mock private TaskDao taskDao;
  @Mock private UserDao userDao;
  @Mock private TaskMapper taskMapper;

  @InjectMocks private TaskServiceImpl taskService;

  private User user;
  private User admin;
  private Task task;
  private TaskResponse taskResponse;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("user@test.com");
    user.setRole(Role.USER);

    admin = new User();
    admin.setId(2L);
    admin.setEmail("admin@test.com");
    admin.setRole(Role.ADMIN);

    task = new Task();
    task.setId(10L);
    task.setTitle("Test Task");
    task.setOwner(user);

    taskResponse = new TaskResponse(10L, "Test Task", null, null, null, null, null, null);
  }

  @Test
  void updateTask_WhenUserOwnsTask_UpdatesTask() {
    TaskRequest request = new TaskRequest("Updated", "Desc", TaskStatus.TODO, null);
    when(taskDao.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(task));
    when(taskDao.save(task)).thenReturn(task);
    when(taskMapper.toResponse(task)).thenReturn(taskResponse);

    TaskResponse result = taskService.updateTask(10L, request, 1L);

    assertNotNull(result);
    verify(taskMapper).updateEntity(request, task);
  }

  @Test
  void updateTask_WhenUserDoesNotOwnTask_ThrowsNotFound() {
    TaskRequest request = new TaskRequest("Updated", "Desc", TaskStatus.TODO, null);
    when(taskDao.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

    assertThrows(
        EntityNotFoundException.class,
        () -> {
          taskService.updateTask(10L, request, 1L);
        });
  }

  @Test
  void getTasks_WhenUserIsAdmin_AppliesNoOwnerRestriction() {
    org.springframework.data.domain.Pageable pageable =
        org.springframework.data.domain.PageRequest.of(0, 10);
    org.springframework.data.domain.Page<Task> page =
        new org.springframework.data.domain.PageImpl<>(java.util.List.of(task));

    // We mock findAll with any Specification
    when(taskDao.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(page);
    when(taskMapper.toResponse(task)).thenReturn(taskResponse);

    PageResponse<TaskResponse> result = taskService.getTasks(null, null, pageable, admin);

    assertNotNull(result);
    assertEquals(1, result.content().size());
  }
}
