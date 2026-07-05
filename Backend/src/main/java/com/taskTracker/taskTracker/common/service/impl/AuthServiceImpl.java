package com.taskTracker.taskTracker.common.service.impl;

import com.taskTracker.taskTracker.common.constant.AuthMessageConstant;
import com.taskTracker.taskTracker.common.exception.ModuleException;
import com.taskTracker.taskTracker.common.model.User;
import com.taskTracker.taskTracker.common.payload.request.LoginRequest;
import com.taskTracker.taskTracker.common.payload.request.RegisterRequest;
import com.taskTracker.taskTracker.common.payload.response.AuthResponse;
import com.taskTracker.taskTracker.common.repository.UserDao;
import com.taskTracker.taskTracker.common.security.JwtUtil;
import com.taskTracker.taskTracker.common.service.AuthService;
import com.taskTracker.taskTracker.common.type.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final AuthenticationManager authenticationManager;

  public AuthServiceImpl(
      UserDao userDao,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      AuthenticationManager authenticationManager) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public AuthResponse register(RegisterRequest request) {
    if (userDao.existsByEmail(request.email())) {
      throw new ModuleException(AuthMessageConstant.AUTH_ERROR_EMAIL_ALREADY_IN_USE, HttpStatus.CONFLICT);
    }

    User user =
        User.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .role(Role.USER) // default role
            .build();

    userDao.save(user);

    String token = jwtUtil.generateToken(user);
    return new AuthResponse(
        token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
  }

  @Override
  public AuthResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user =
        userDao
            .findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    String token = jwtUtil.generateToken(user);
    return new AuthResponse(
        token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
  }
}
