package com.taskTracker.taskTracker.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthMessageConstant implements MessageConstant {

  AUTH_ERROR_EMAIL_ALREADY_IN_USE("Email already in use"),
  AUTH_ERROR_INVALID_CREDENTIALS("Invalid email or password");

  private final String message;
}
