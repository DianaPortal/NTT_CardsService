package com.nttdata.cards_service.model.auth;

import lombok.*;
import lombok.experimental.*;

@Data
@Accessors(chain = true)
public class LoginResponse {
  private String tokenType; // "Bearer"
  private String token;
}
