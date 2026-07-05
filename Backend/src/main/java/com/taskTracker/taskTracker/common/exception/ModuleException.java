package com.taskTracker.taskTracker.common.exception;

import com.taskTracker.taskTracker.common.constant.MessageConstant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ModuleException extends RuntimeException {

  private final HttpStatus status;

  public ModuleException(MessageConstant messageKey) {
    this(messageKey, HttpStatus.BAD_REQUEST);
  }

  public ModuleException(MessageConstant messageKey, HttpStatus status) {
    super(messageKey.getMessage());
    this.status = status;
  }
}
