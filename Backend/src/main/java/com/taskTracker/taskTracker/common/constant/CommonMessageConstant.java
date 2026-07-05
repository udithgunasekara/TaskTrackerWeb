package com.taskTracker.taskTracker.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonMessageConstant implements MessageConstant {

  COMMON_ERROR_VALIDATION_ERROR("Validation failed"),
  COMMON_ERROR_ACCESS_DENIED("Access is denied"),
  COMMON_ERROR_UNAUTHORIZED_ACCESS("Authentication required"),
  COMMON_ERROR_DATABASE_ERROR("A database error occurred"),
  COMMON_ERROR_INTERNAL_ERROR("An unexpected error occurred");

  private final String message;
}
