package com.taskTracker.taskTracker.task.controller;

import com.taskTracker.taskTracker.common.payload.response.ResponseEntityDto;
import com.taskTracker.taskTracker.common.security.CustomUserDetails;
import com.taskTracker.taskTracker.task.payload.request.TaskRequest;
import com.taskTracker.taskTracker.task.service.TaskService;
import com.taskTracker.taskTracker.task.type.TaskStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
  public ResponseEntity<ResponseEntityDto> createTask(
      @Valid @RequestBody TaskRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
    var response = taskService.createTask(request, userDetails.getUser().getId());
    return ResponseEntity.ok(new ResponseEntityDto(false, response));
  }

  @GetMapping
  public ResponseEntity<ResponseEntityDto> getTasks(
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) Long ownerId,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    var response = taskService.getTasks(status, ownerId, pageable, userDetails.getUser());
    return ResponseEntity.ok(new ResponseEntityDto(false, response));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ResponseEntityDto> updateTask(
      @PathVariable Long id,
      @Valid @RequestBody TaskRequest request,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    var response = taskService.updateTask(id, request, userDetails.getUser().getId());
    return ResponseEntity.ok(new ResponseEntityDto(false, response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(
      @PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
    taskService.deleteTask(id, userDetails.getUser().getId());
    return ResponseEntity.noContent().build();
  }
}
