package com.taskTracker.taskTracker.common.payload.response;

import java.util.List;

public class ResponseEntityDto {

  private static final String SUCCESSFUL = "successful";
  private static final String UNSUCCESSFUL = "unsuccessful";

  private final String status;
  private final List<Object> results;

  public ResponseEntityDto(boolean isError, Object data) {
    this.status = isError ? UNSUCCESSFUL : SUCCESSFUL;
    this.results = data == null ? List.of() : List.of(data);
  }

  public String getStatus() {
    return status;
  }

  public List<Object> getResults() {
    return results;
  }
}
