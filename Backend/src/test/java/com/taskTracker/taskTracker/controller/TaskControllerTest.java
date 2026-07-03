package com.taskTracker.taskTracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskTracker.taskTracker.dto.request.TaskRequest;
import com.taskTracker.taskTracker.dto.response.TaskResponse;
import com.taskTracker.taskTracker.entity.TaskStatus;
import com.taskTracker.taskTracker.security.JwtAuthenticationFilter;
import com.taskTracker.taskTracker.security.JwtUtil;
import com.taskTracker.taskTracker.security.RestAuthEntryPoint;
import com.taskTracker.taskTracker.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false) // disables security filters for unit test
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private TaskService taskService;
  @MockBean private JwtUtil jwtUtil; // in case it's needed by context

  @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
  @MockBean private UserDetailsService userDetailsService;
  @MockBean private RestAuthEntryPoint restAuthEntryPoint;

  @Test
  @WithMockUser
  void createTask_ValidInput_Returns200() throws Exception {
    TaskRequest request = new TaskRequest("Test Title", "Desc", TaskStatus.TODO, null);
    TaskResponse response =
        new TaskResponse(1L, "Test Title", "Desc", TaskStatus.TODO, null, null, null, null);

    when(taskService.createTask(any(TaskRequest.class), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Title"));
  }

  @Test
  @WithMockUser
  void createTask_InvalidInput_Returns400() throws Exception {
    TaskRequest request = new TaskRequest("", "Desc", TaskStatus.TODO, null); // invalid blank title

    mockMvc
        .perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
