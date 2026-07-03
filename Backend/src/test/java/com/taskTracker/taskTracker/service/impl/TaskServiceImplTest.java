package com.taskTracker.taskTracker.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.entity.Role;
import com.taskTracker.taskTracker.entity.Task;
import com.taskTracker.taskTracker.entity.User;
import com.taskTracker.taskTracker.event.TaskEventPublisher;
import com.taskTracker.taskTracker.exception.ResourceNotFoundException;
import com.taskTracker.taskTracker.mapper.TaskMapper;
import com.taskTracker.taskTracker.repository.TaskRepository;
import com.taskTracker.taskTracker.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

  @Mock private TaskRepository taskRepository;
  @Mock private UserRepository userRepository;
  @Mock private TaskMapper taskMapper;
  @Mock private TaskEventPublisher taskEventPublisher;

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
    com.taskTracker.taskTracker.dto.request.TaskRequest request =
        new com.taskTracker.taskTracker.dto.request.TaskRequest(
            "Updated", "Desc", com.taskTracker.taskTracker.entity.TaskStatus.TODO, null);
    when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(task));
    when(taskRepository.save(task)).thenReturn(task);
    when(taskMapper.toResponse(task)).thenReturn(taskResponse);

    TaskResponse result = taskService.updateTask(10L, request, 1L);

    assertNotNull(result);
    verify(taskMapper).updateEntity(request, task);
    verify(taskEventPublisher)
        .publishEvent(eq(com.taskTracker.taskTracker.event.TaskEventType.UPDATED), any());
  }

  @Test
  void updateTask_WhenUserDoesNotOwnTask_ThrowsNotFound() {
    com.taskTracker.taskTracker.dto.request.TaskRequest request =
        new com.taskTracker.taskTracker.dto.request.TaskRequest(
            "Updated", "Desc", com.taskTracker.taskTracker.entity.TaskStatus.TODO, null);
    when(taskRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
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
    when(taskRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
        .thenReturn(page);
    when(taskMapper.toResponse(task)).thenReturn(taskResponse);

    com.taskTracker.taskTracker.dto.response.PageResponse<TaskResponse> result =
        taskService.getTasks(null, null, pageable, admin);

    assertNotNull(result);
    assertEquals(1, result.content().size());
  }
}
