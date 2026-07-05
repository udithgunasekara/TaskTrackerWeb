package com.taskTracker.taskTracker.common.exception;

import com.taskTracker.taskTracker.common.constant.MessageConstant;

public class EntityNotFoundException extends RuntimeException {

  public EntityNotFoundException(MessageConstant messageKey) {
    super(messageKey.getMessage());
  }
}
