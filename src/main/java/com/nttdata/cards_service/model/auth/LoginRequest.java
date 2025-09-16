package com.nttdata.cards_service.model.auth;

import lombok.*;

@Data
public class LoginRequest {
  private String username;
  private String password;
}
