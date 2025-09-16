package com.nttdata.cards_service.security;

import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;
import reactor.core.publisher.*;

import java.util.*;
import java.util.stream.*;

public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  private final JwtService jwt;

  public JwtReactiveAuthenticationManager(JwtService jwt) {
    this.jwt = jwt;
  }

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    String token = (String) authentication.getCredentials();

    if (!jwt.isValid(token)) {
      return Mono.error(new BadCredentialsException("Invalid JWT"));
    }

    String username = jwt.getUsername(token);
    List<SimpleGrantedAuthority> authorities = jwt.getRoles(token).stream()
        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());

    JwtPreAuthenticatedToken authenticated = new JwtPreAuthenticatedToken(token, authorities);
    authenticated.setDetails(username);
    return Mono.just(authenticated);
  }
}