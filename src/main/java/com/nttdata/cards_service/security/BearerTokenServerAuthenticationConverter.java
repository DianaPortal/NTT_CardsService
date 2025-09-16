package com.nttdata.cards_service.security;


import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.security.web.server.authentication.*;
import org.springframework.web.server.*;
import reactor.core.publisher.*;


public class BearerTokenServerAuthenticationConverter implements ServerAuthenticationConverter {
  @Override
  // Extrae el token Bearer del header Authorization
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (auth == null || !auth.startsWith("Bearer "))
      return Mono.empty();
    String token = auth.substring(7);
    return Mono.just(new JwtPreAuthenticatedToken(token));
  }
}
