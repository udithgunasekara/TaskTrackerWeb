package com.taskTracker.taskTracker.common.exception;

import com.taskTracker.taskTracker.common.constant.CommonMessageConstant;
import com.taskTracker.taskTracker.common.payload.response.ErrorResponse;
import com.taskTracker.taskTracker.common.payload.response.ResponseEntityDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseEntityDto> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            CommonMessageConstant.COMMON_ERROR_VALIDATION_ERROR.getMessage(),
            request.getRequestURI(),
            errors);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ResponseEntityDto> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Malformed request body",
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ResponseEntityDto> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Type mismatch for parameter: " + ex.getName(),
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ResponseEntityDto> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            "Unauthorized",
            "Invalid email or password",
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(ModuleException.class)
  public ResponseEntity<ResponseEntityDto> handleModuleException(
      ModuleException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), ex.getStatus());
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ResponseEntityDto> handleEntityNotFound(
      EntityNotFoundException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ResponseEntityDto> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "Access is denied",
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(new ResponseEntityDto(true, response), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseEntityDto> handleAllOtherExceptions(
      Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    ErrorResponse response =
        new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            CommonMessageConstant.COMMON_ERROR_INTERNAL_ERROR.getMessage(),
            request.getRequestURI(),
            null);
    return new ResponseEntity<>(
        new ResponseEntityDto(true, response), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
