package com.nttdata.cards_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class CustomReactiveJwtDecoder implements ReactiveJwtDecoder {

  private final List<String> allowedIssuers;

  public CustomReactiveJwtDecoder(@Value("${app.auth.allowed-issuers}") String issuers) {
    this.allowedIssuers = Arrays.asList(issuers.split(","));
  }

  @Override
  public Mono<Jwt> decode(String token) throws JwtException {
    return tryDecodeWithIssuers(token, 0);
  }

  private Mono<Jwt> tryDecodeWithIssuers(String token, int index) {
    if (index >= allowedIssuers.size()) {
      return Mono.error(new JwtValidationException("No valid issuer found for token", null));
    }

    String issuer = allowedIssuers.get(index);
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
        .withJwkSetUri(issuer + "/protocol/openid-connect/certs")
        .build();

    return decoder.decode(token)
        .onErrorResume(JwtException.class, e -> {
          // Intentar con el siguiente issuer
          return tryDecodeWithIssuers(token, index + 1);
        });
  }
}