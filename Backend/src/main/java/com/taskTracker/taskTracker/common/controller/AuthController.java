package com.taskTracker.taskTracker.common.controller;

import com.taskTracker.taskTracker.common.payload.request.LoginRequest;
import com.taskTracker.taskTracker.common.payload.request.RegisterRequest;
import com.taskTracker.taskTracker.common.payload.response.ResponseEntityDto;
import com.taskTracker.taskTracker.common.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<ResponseEntityDto> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(new ResponseEntityDto(false, authService.register(request)));
  }

  @PostMapping("/login")
  public ResponseEntity<ResponseEntityDto> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(new ResponseEntityDto(false, authService.login(request)));
  }
}
