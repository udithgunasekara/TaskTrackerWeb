package com.taskTracker.taskTracker.controller;

import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.PageResponse;
import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.security.CustomUserDetails;
import com.taskTracker.taskTracker.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> createTask(
      @Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(taskService.createTask(request, userDetails.getUser().getId()));
  }

  @GetMapping
  public ResponseEntity<PageResponse<TaskResponse>> getTasks(
      @RequestParam(required = false) com.taskTracker.taskTracker.entity.TaskStatus status,
      @RequestParam(required = false) Long ownerId,
      @org.springframework.data.web.PageableDefault(
              sort = "createdAt",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(
        taskService.getTasks(status, ownerId, pageable, userDetails.getUser()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TaskResponse> updateTask(
      @PathVariable Long id,
      @Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(taskService.updateTask(id, request, userDetails.getUser().getId()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
    taskService.deleteTask(id, userDetails.getUser().getId());
    return ResponseEntity.noContent().build();
  }
}
