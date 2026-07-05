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
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(taskService.getTasks(userDetails.getUser().getId(), page, size));
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
