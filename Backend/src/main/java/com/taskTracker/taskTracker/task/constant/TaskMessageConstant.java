package com.taskTracker.taskTracker.task.constant;

import com.taskTracker.taskTracker.common.constant.MessageConstant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TaskMessageConstant implements MessageConstant {

  TASK_NOT_FOUND("Task not found");

  private final String message;
}
