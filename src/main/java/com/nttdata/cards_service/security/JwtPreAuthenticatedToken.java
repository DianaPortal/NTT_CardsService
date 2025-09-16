package com.nttdata.cards_service.security;

import org.springframework.security.authentication.*;
import org.springframework.security.core.*;

import java.util.*;

public class JwtPreAuthenticatedToken extends AbstractAuthenticationToken {
  private final String token;

  public JwtPreAuthenticatedToken(String token) {
    super(null);
    this.token = token;
    setAuthenticated(false);
  }

  public JwtPreAuthenticatedToken(String token, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.token = token;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getPrincipal() {
    return null;
  }
}