package com.taskTracker.taskTracker.service;

import com.taskTracker.taskTracker.dto.request.LoginRequest;
import com.taskTracker.taskTracker.dto.request.RegisterRequest;
import com.taskTracker.taskTracker.dto.response.AuthResponse;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);
}
