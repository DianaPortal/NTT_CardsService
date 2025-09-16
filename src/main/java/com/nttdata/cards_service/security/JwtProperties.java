package com.nttdata.cards_service.security;

import lombok.*;
import org.springframework.boot.context.properties.*;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
  private String secret;
  private long expiration;   // segundos
  private String issuer;
}