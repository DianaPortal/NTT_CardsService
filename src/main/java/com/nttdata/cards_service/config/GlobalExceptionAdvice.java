package com.nttdata.cards_service.config;


import com.nttdata.cards_service.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionAdvice {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleRse(ResponseStatusException ex) {
    ErrorResponse body = new ErrorResponse()
        .timestamp(OffsetDateTime.now())
        .status(ex.getStatus().value())
        .error(ex.getStatus().getReasonPhrase())
        .message(ex.getReason());
    return ResponseEntity.status(ex.getStatus()).body(body);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ErrorResponse> handleOther(Throwable ex) {
    ErrorResponse body = new ErrorResponse()
        .timestamp(OffsetDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message("INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}